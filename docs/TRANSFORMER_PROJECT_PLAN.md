# RJSF Form Transformer Microservice — Detailed Project Plan

**Document Version:** 1.0
**Status:** Implementation Complete
**Date:** 2026-04-06
**Based On:** `rjsf-transformer-microservice-spec-java17.docx` v2.0
**Runtime:** Spring Boot 3.4.4 / Java 17 / PostgreSQL 16

---

## 1. Executive Summary

The RJSF Form Transformer Microservice is a subsystem within the existing EGS RJSF Form Service that provides **bidirectional JSON-to-relational transformation** for RJSF form submissions. It accepts completed RJSF `formData` payloads, flattens them into discrete, typed PostgreSQL columns using per-form **Transformer Template** configuration files, and reconstructs them on read.

The subsystem is entirely **data-driven**: no application code changes are required when a new form is introduced. Only a new `.transformer.json` template file is deployed.

### Key Capabilities

- **Write Path** — Accept formData, validate fields, apply transforms, and persist into typed relational columns
- **Read Path** — Query relational data, apply reverse transforms, and reconstruct RJSF-compatible formData JSON
- **Dynamic DDL** — Automatically create and reconcile per-form PostgreSQL tables from template definitions
- **Pluggable Hooks** — Named Spring beans invoked at pipeline stages for custom business logic
- **Hot Reload** — File-system-watched template directory enables schema changes without service restart
- **Extensible Transforms** — 13 built-in transforms with support for custom transforms via annotation

---

## 2. Architecture Overview

### 2.1 Position in the Ecosystem

The Transformer subsystem sits alongside the existing JSONB-based form submission storage. Both coexist within the same Spring Boot application. The new subsystem is isolated under the `com.egs.rjsf.transformer` package namespace and exposes its own REST endpoints at `/api/v1/submissions`.

```
┌─────────────────────────────────────────────────────────────┐
│              RJSF Host Application (React Frontend)         │
│           Form rendered by react-jsonschema-form            │
└──────────┬──────────────────────────────┬───────────────────┘
           │                              │
           │ Existing API                 │ Transformer API
           │ /api/form-submissions        │ /api/v1/submissions
           ▼                              ▼
┌──────────────────────┐   ┌──────────────────────────────────┐
│  Existing Services   │   │  Transformer Subsystem           │
│  (JSONB blob storage)│   │  ┌────────────┐ ┌─────────────┐ │
│                      │   │  │ Template   │ │ Transformer │ │
│  FormSubmissionSvc   │   │  │ Registry   │→│ Engine      │ │
│  AwardService        │   │  └────────────┘ └──────┬──────┘ │
│  SchemaVersionSvc    │   │                        │        │
└──────────────────────┘   │                  NamedJdbc      │
           │               │                        ▼        │
           ▼               │               ┌─────────────┐   │
    ┌─────────────┐        │               │ Per-Form     │   │
    │ PostgreSQL  │◄───────┤               │ Tables       │   │
    │ (JSONB cols)│        │               │ (typed cols) │   │
    └─────────────┘        │               └─────────────┘   │
                           └──────────────────────────────────┘
```

### 2.2 Component Inventory

| Component | Package | Responsibility | Spring Type |
|-----------|---------|----------------|-------------|
| **Template Registry** | `registry` | Loads, caches, validates, and hot-reloads transformer JSON template files | `@Component` + `ConcurrentHashMap` |
| **Template Loader** | `registry` | Reads template files from classpath or filesystem; watches for changes | `@Component` + `SmartLifecycle` |
| **Template Validator** | `registry` | Programmatic validation of template structure on load | `@Component` |
| **Transform Registry** | `transform` | Discovers and stores named transform functions via classpath scanning | `@Component` + `@PostConstruct` |
| **Built-In Transforms** | `transform` | 13 transforms for common type conversions (dates, JSON, numbers, etc.) | `@TransformFunction` inner classes |
| **Path Resolver** | `engine` | Dot-notation get/set on nested `Map<String,Object>` structures | `@Component` |
| **Type Coercion Service** | `engine` | Converts Java values to/from SQL column types | `@Component` |
| **DDL Manager** | `ddl` | Creates or reconciles per-form tables on template load/update | `@Component` + `JdbcTemplate` |
| **SQL Executor** | `db` | Executes typed INSERT and SELECT statements against per-form tables | `@Component` + `NamedParameterJdbcTemplate` |
| **Validation Gate** | `validation` | Validates inbound formData against template field constraints | `@Component` |
| **Hook Registry** | `hook` | Resolves hook beans by name from Spring ApplicationContext | `@Component` |
| **Write Service** | `service` | Orchestrates the 7-stage write pipeline | `@Service` + `@Transactional` |
| **Read Service** | `service` | Orchestrates the 7-stage read pipeline | `@Service` |
| **REST Controller** | `controller` | POST and GET endpoints for transformer submissions | `@RestController` |
| **Exception Handler** | `config` | Maps transformer exceptions to HTTP status codes | `@RestControllerAdvice` |
| **Cache Config** | `config` | Caffeine cache manager with configurable max size | `@Configuration` + `@EnableCaching` |
| **Properties** | `config` | Type-safe binding for `transformer.*` application properties | `@ConfigurationProperties` |

---

## 3. Transformer Template Specification

### 3.1 File Location and Naming

- **Default directory:** `classpath:templates` (overridden via `TRANSFORMER_TEMPLATE_DIR` env var)
- **File naming:** `{formId}.transformer.json` (e.g., `animal-research.transformer.json`)
- **Resolution:** The registry matches the `formId` field in the API request to the filename prefix

### 3.2 Top-Level Template Structure

```json
{
  "formId":        "employee-onboarding",
  "version":       2,
  "tableName":     "form_employee_onboarding",
  "description":   "New employee onboarding form",
  "schemaVersion": { "column": "schema_version", "sqlType": "INTEGER" },
  "auditColumns":  { "createdAt": "created_at", "updatedAt": "updated_at", "submittedBy": "submitted_by" },
  "fields":        [ /* FieldMapping objects */ ],
  "relations":     [ /* RelationMapping objects */ ],
  "writeHooks":    { "preValidation": [], "preInsert": [], "postInsert": [] },
  "readHooks":     { "postQuery": [], "postAssemble": [] }
}
```

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `formId` | String | Yes | Unique identifier matching filename prefix |
| `version` | int | Yes | Template version, stored alongside every row |
| `tableName` | String | Yes | Target PostgreSQL table name (convention: `form_{formId_snake}`) |
| `description` | String | No | Human-readable description |
| `schemaVersion` | Object | Yes | Column name and SQL type for version tracking |
| `auditColumns` | Object | Yes | Maps audit field names to physical column names |
| `fields` | Array | Yes | FieldMapping objects for JSON-to-column mapping |
| `relations` | Array | No | RelationMapping objects for array/child tables |
| `writeHooks` | Object | No | Named hook beans for write pipeline stages |
| `readHooks` | Object | No | Named hook beans for read pipeline stages |

### 3.3 Field Mapping Object

```json
{
  "jsonPath":     "personal.email",
  "column":       "email",
  "sqlType":      "TEXT",
  "nullable":     false,
  "defaultValue": null,
  "transform":    { "toDb": "toLowerCase", "fromDb": null },
  "validation":   { "type": "string", "format": "email" },
  "tags":         ["pii"]
}
```

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `jsonPath` | String | — | Dot-notation path into formData (e.g., `address.city`) |
| `column` | String | — | Target PostgreSQL column name |
| `sqlType` | String | — | Column type: TEXT, INTEGER, NUMERIC, BOOLEAN, DATE, TIMESTAMPTZ, JSONB, UUID, VARCHAR |
| `nullable` | boolean | `true` | If false, Validation Gate rejects absent values |
| `defaultValue` | any | `null` | Value used when JSON field is absent |
| `transform` | Object | `null` | Named transform functions for write (`toDb`) and read (`fromDb`) |
| `validation` | Object | `null` | JSON Schema fragment for field-level validation |
| `tags` | Array | `[]` | Arbitrary labels for PII auditing or downstream processing |

### 3.4 Relation Mapping (Child Tables for Arrays)

```json
{
  "jsonPath":    "certifications",
  "childTable":  "form_employee_onboarding_certifications",
  "parentKey":   "submission_id",
  "orderColumn": "item_index",
  "fields":      [ /* FieldMapping objects scoped to array elements */ ]
}
```

Child tables store arrays of sub-objects. Each child row includes:
- A foreign key (`parentKey`) referencing the parent table's `submission_id`
- An integer `orderColumn` preserving array order for reconstruction on read

### 3.5 Hook Points

Hooks are named Spring beans implementing `WriteHook` or `ReadHook` functional interfaces. Templates reference them by bean name; the `HookRegistry` resolves and invokes them at the appropriate pipeline stage.

| Hook Point | Path | Interface | Receives | Returns |
|------------|------|-----------|----------|---------|
| `preValidation` | Write | `WriteHook` | Raw formData map | Transformed formData |
| `preInsert` | Write | `WriteHook` | Assembled column-value map | Modified column-value map |
| `postInsert` | Write | `WriteHook` | Final column-value map | Ignored (side-effects only) |
| `postQuery` | Read | `ReadHook` | Raw database row map | Modified row map |
| `postAssemble` | Read | `ReadHook` | Reconstructed formData map | Final formData |

Missing hook beans are logged at WARN level and skipped (no failure).

### 3.6 Built-In Transform Functions

| Transform Name | Direction | Behavior |
|---------------|-----------|----------|
| `toLocalDate` | toDb | Parse ISO-8601 date string to `java.time.LocalDate` |
| `toIsoDateString` | fromDb | Format `LocalDate` or `java.sql.Date` to `yyyy-MM-dd` |
| `toInstant` | toDb | Parse ISO-8601 datetime string to `java.time.Instant` |
| `fromInstant` | fromDb | Format `Instant` to ISO-8601 datetime string |
| `jsonStringify` | toDb | Serialize Map or List to JSON string for JSONB columns |
| `jsonParse` | fromDb | Deserialize JSON string to Map or List |
| `trimString` | toDb | `String.strip()` before insert |
| `toLowerCase` | toDb | `String.toLowerCase(Locale.ROOT)` for case-normalized storage |
| `toBoolean` | toDb | Coerce "true"/"false", 1/0, Boolean to Boolean |
| `toBigDecimal` | toDb | Convert String or Number to BigDecimal |
| `toInteger` | toDb | Convert String or Number to Integer |
| `maskLast4` | fromDb | Mask string retaining only last 4 characters |
| `toUUID` | toDb | Validate and normalize UUID via `UUID.fromString()` |

**Extensibility:** Custom transforms are registered by creating a class annotated with `@TransformFunction("name")` that implements `ToDbFunction` and/or `FromDbFunction`. The `TransformRegistry` discovers them automatically at startup via `@Component` scanning.

---

## 4. Write Path — formData to Relational Table

### 4.1 API Endpoint

```
POST /api/v1/submissions
Content-Type: application/json

{
  "form_id":         "employee-onboarding",
  "schema_version":  2,
  "submitted_by":    "user-uuid-here",
  "form_data": {
    "personal": { "firstName": "Jane", "lastName": "Smith", ... },
    "certifications": [ { "name": "AWS", ... } ]
  }
}
```

**Success Response — HTTP 201:**
```json
{
  "submission_id": 4821,
  "form_id":       "employee-onboarding",
  "version":       2,
  "created_at":    "2026-04-06T14:32:00.000-04:00",
  "form_data":     null
}
```

**Error Responses:**

| HTTP | Condition | Error Code |
|------|-----------|------------|
| 400 | formId missing or template not found | `TEMPLATE_NOT_FOUND` |
| 400 | Validation gate failure | `VALIDATION_FAILED` |
| 400 | schemaVersion newer than template version | `VERSION_MISMATCH` |
| 409 | Unique constraint violation on target table | `DUPLICATE_SUBMISSION` |

### 4.2 Write Pipeline Stages

The write pipeline executes 7 stages in sequence within a `@Transactional(isolation = REPEATABLE_READ)` boundary.

```
 Request
    │
    ▼
┌──────────────────────┐
│ Stage 1: Template    │  TemplateRegistry.getTemplate(formId)
│ Resolution           │  Throws TemplateNotFoundException if missing
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 2: Pre-        │  Execute writeHooks.preValidation beans
│ Validation Hooks     │  Normalize phone numbers, trim whitespace, etc.
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 3: Validation  │  Check nullable=false fields present
│ Gate                 │  Validate enum constraints, email/date formats
│                      │  Collect ALL violations before throwing
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 4: Field       │  For each FieldMapping:
│ Mapping              │    1. Extract value via PathResolver (dot-notation)
│                      │    2. Apply toDb transform from TransformRegistry
│                      │    3. Coerce to SQL type via TypeCoercionService
│                      │    4. Populate column-value map
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 5: Pre-Insert  │  Execute writeHooks.preInsert beans
│ Hooks                │  Field hashing, encryption, cross-field logic
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 6: SQL Insert  │  INSERT parent row (returns submission_id)
│ (Transactional)      │  Batch INSERT child rows for each RelationMapping
│                      │  DuplicateKeyException → DuplicateSubmissionException
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 7: Post-Insert │  Execute writeHooks.postInsert beans
│ Hooks                │  Audit events, search index updates
│                      │  Failures logged but do not roll back
└──────────┬───────────┘
           ▼
   Response (201)
```

---

## 5. Read Path — Relational Table to formData

### 5.1 API Endpoint

```
GET /api/v1/submissions/4821?form_id=employee-onboarding
Accept: application/json
```

Optional: `?template_version=2` to pin to a specific template version for historical reads.

**Success Response — HTTP 200:**
```json
{
  "submission_id": 4821,
  "form_id":       "employee-onboarding",
  "version":       2,
  "created_at":    "2026-04-06T14:32:00.000-04:00",
  "form_data": {
    "personal": { "firstName": "Jane", "lastName": "Smith", ... },
    "certifications": [ { "name": "AWS", ... } ]
  }
}
```

**Error Responses:**

| HTTP | Condition | Error Code |
|------|-----------|------------|
| 400 | formId missing or template not found | `TEMPLATE_NOT_FOUND` |
| 404 | No row found for submissionId | `SUBMISSION_NOT_FOUND` |

### 5.2 Read Pipeline Stages

```
 Request
    │
    ▼
┌──────────────────────┐
│ Stage 1: Template    │  TemplateRegistry.getTemplate(formId)
│ Resolution           │  Optional version pinning via TemplateHistoryRepository
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 2: Primary     │  SELECT submission_id, schema_version, all field columns
│ Row Query            │  FROM {tableName} WHERE submission_id = :id
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 3: Child       │  For each RelationMapping:
│ Table Queries        │    SELECT ... FROM {childTable}
│                      │    WHERE submission_id = :id ORDER BY item_index
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 4: Post-Query  │  Execute readHooks.postQuery beans
│ Hooks                │  PII masking, decryption, conditional suppression
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 5: Field       │  For each FieldMapping:
│ Assembly             │    1. Get column value from row
│                      │    2. Apply fromDb transform
│                      │    3. Set at jsonPath in output map via PathResolver
│                      │  For each RelationMapping:
│                      │    Assemble child rows into nested lists
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 6: Post-       │  Execute readHooks.postAssemble beans
│ Assembly Hooks       │  Computed fields, lookup hydration, UI metadata
└──────────┬───────────┘
           ▼
┌──────────────────────┐
│ Stage 7: Response    │  Wrap formData in SubmissionResponse with
│ Envelope             │  submissionId, formId, schemaVersion, createdAt
└──────────┬───────────┘
           ▼
   Response (200)
```

---

## 6. DDL Management — Dynamic Table Lifecycle

The `DdlManager` component manages per-form table creation and schema reconciliation at startup and on template hot-reload.

### 6.1 Startup Behavior

1. `TemplateRegistry` loads all `*.transformer.json` files from the template directory
2. For each template, `DdlManager.reconcile(template)` is called
3. If a table does not exist, it is created via a dynamically constructed `CREATE TABLE`
4. If a table exists but is missing columns, the missing columns are added via `ALTER TABLE`
5. Columns present in the database but absent from the template are **never dropped** — they are logged as WARN-level deprecation notices

### 6.2 Generated DDL Examples

**Parent table creation:**
```sql
CREATE TABLE form_employee_onboarding (
    submission_id    BIGSERIAL PRIMARY KEY,
    schema_version   INTEGER NOT NULL,
    first_name       TEXT NOT NULL,
    last_name        TEXT NOT NULL,
    email            TEXT NOT NULL,
    start_date       DATE NOT NULL,
    department       TEXT NOT NULL,
    job_title        TEXT NOT NULL,
    salary           NUMERIC,
    is_remote        BOOLEAN NOT NULL,
    equipment_prefs  JSONB,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_by     TEXT
);
```

**Child table creation:**
```sql
CREATE TABLE form_employee_onboarding_certifications (
    id             BIGSERIAL PRIMARY KEY,
    submission_id  BIGINT NOT NULL REFERENCES form_employee_onboarding(submission_id) ON DELETE CASCADE,
    item_index     INTEGER NOT NULL,
    cert_name      TEXT NOT NULL,
    issued_by      TEXT,
    expiry_date    DATE
);
```

### 6.3 DDL Safety Rules

| Rule | Enforcement |
|------|-------------|
| **Never DROP** | DdlManager will never issue `DROP COLUMN` or `DROP TABLE`. Deprecated columns are logged only. |
| **New columns always nullable** | All `ALTER TABLE ADD COLUMN` statements omit `NOT NULL` to prevent table rewrites on large datasets. |
| **Identifier validation** | All table and column names validated against `^[a-zA-Z_][a-zA-Z0-9_]{0,62}$` regex before SQL execution. |
| **SQL type whitelist** | Only allowed types: TEXT, INTEGER, BIGINT, SMALLINT, BOOLEAN, NUMERIC, DECIMAL, REAL, DOUBLE PRECISION, DATE, TIME, TIMESTAMP, TIMESTAMPTZ, UUID, JSONB, JSON, VARCHAR, CHAR, BYTEA, SERIAL, BIGSERIAL |
| **No user input in SQL** | All dynamic SQL uses validated identifiers only; data values go through named parameters. |

---

## 7. Template Registry and Hot Reload

### 7.1 Caching

The `TemplateRegistry` wraps a `ConcurrentHashMap<String, TransformerTemplate>` keyed by formId. The cache is configured with a configurable maximum size (default 500). There is no automatic expiry — entries are invalidated on file-change events or programmatically.

### 7.2 Hot Reload

When `transformer.template.hot-reload=true` and the template directory is a filesystem path (not classpath), a background daemon thread uses `java.nio.file.WatchService` to monitor the directory:

1. On `ENTRY_MODIFY` or `ENTRY_CREATE`, the changed template file is re-parsed and validated
2. If validation succeeds, the cache entry is replaced atomically
3. `DdlManager.reconcile()` is called for the affected form (adds any new columns)
4. In-flight requests using the old template complete normally (records are immutable)

Hot-reload is automatically disabled for classpath resources (logged at INFO level).

### 7.3 Template Version History

Each successfully loaded template is persisted to the `transformer_template_history` table. This enables historical reads — when a submission was written with template version N, it can be read back using that version's field mappings.

```sql
CREATE TABLE transformer_template_history (
    id              BIGSERIAL PRIMARY KEY,
    form_id         TEXT        NOT NULL,
    version         INTEGER     NOT NULL,
    template_json   JSONB       NOT NULL,
    loaded_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (form_id, version)
);
```

---

## 8. Project Structure

All new code lives under `com.egs.rjsf.transformer` to cleanly separate from the existing `com.egs.rjsf` code. No existing files were refactored.

```
service/src/main/java/com/egs/rjsf/transformer/
├── config/
│   ├── CacheConfig.java                     # @EnableCaching + Caffeine CacheManager
│   ├── TransformerExceptionHandler.java      # @RestControllerAdvice (scoped to transformer)
│   └── TransformerProperties.java            # @ConfigurationProperties(prefix = "transformer")
├── controller/
│   └── TransformerSubmissionController.java  # POST + GET at /api/v1/submissions
├── db/
│   └── SqlExecutor.java                     # NamedParameterJdbcTemplate wrappers
├── ddl/
│   └── DdlManager.java                     # Dynamic CREATE TABLE / ALTER TABLE
├── dto/
│   ├── TransformerSubmissionRequest.java     # Request record
│   └── TransformerSubmissionResponse.java    # Response record
├── engine/
│   ├── PathResolver.java                    # Dot-notation get/set on Map<String,Object>
│   └── TypeCoercionService.java             # Java value → SQL type coercion
├── entity/
│   └── TemplateHistoryEntity.java           # JPA entity for template version history
├── exception/
│   ├── DuplicateSubmissionException.java
│   ├── SubmissionNotFoundException.java
│   ├── TemplateNotFoundException.java
│   ├── TransformerException.java            # Base exception with error code
│   ├── TransformerValidationException.java  # Carries List<Violation>
│   └── VersionMismatchException.java
├── hook/
│   ├── HookRegistry.java                   # Resolves hook beans by name
│   ├── ReadHook.java                        # @FunctionalInterface
│   └── WriteHook.java                       # @FunctionalInterface
├── model/
│   ├── AuditColumnConfig.java               # Record: createdAt, updatedAt, submittedBy
│   ├── FieldMapping.java                    # Record: jsonPath, column, sqlType, ...
│   ├── HookConfig.java                      # Record: 5 hook point lists
│   ├── RelationMapping.java                 # Record: child table definition
│   ├── SchemaVersionConfig.java             # Record: column, sqlType
│   ├── TransformConfig.java                 # Record: toDb, fromDb
│   └── TransformerTemplate.java             # Record: top-level template model
├── registry/
│   ├── FileSystemTemplateLoader.java        # Classpath/filesystem loader + WatchService
│   ├── TemplateLoader.java                  # Interface: load, list, watch
│   ├── TemplateRegistry.java               # ConcurrentHashMap cache + hot-reload
│   └── TemplateValidator.java               # Programmatic template validation
├── repository/
│   └── TemplateHistoryRepository.java       # JpaRepository for template history
├── service/
│   ├── SubmissionReadService.java           # 7-stage read pipeline
│   └── SubmissionWriteService.java          # 7-stage write pipeline
├── transform/
│   ├── BuiltInTransforms.java              # 13 named transforms (inner @Component classes)
│   ├── FromDbFunction.java                  # @FunctionalInterface
│   ├── ToDbFunction.java                    # @FunctionalInterface
│   ├── TransformFunction.java               # @interface (meta-annotated @Component)
│   └── TransformRegistry.java              # Discovers @TransformFunction beans
├── util/
│   └── IdentifierValidator.java             # SQL identifier regex validation
└── validation/
    ├── ValidationGate.java                  # Field validation (required, enum, format)
    └── Violation.java                        # Record: jsonPath, message

service/src/main/resources/
├── application.yml                          # Updated with transformer.* properties
├── db/
│   ├── init.sql                             # Updated with transformer_template_history DDL
│   └── migrations/
│       ├── 001_add_versioning_tables.sql    # (pre-existing)
│       ├── 002_seed_version_data.sql        # (pre-existing)
│       └── 003_create_transformer_template_history.sql  # NEW
└── templates/
    ├── animal-research.transformer.json      # Sample: flat form with JSONB array
    └── employee-onboarding.transformer.json  # Sample: nested fields + child table + hooks
```

**Total new Java files:** 42
**Total new resource files:** 3 (SQL migration + 2 templates)
**Modified existing files:** 3 (pom.xml, application.yml, init.sql)

---

## 9. Dependencies

### 9.1 New Maven Dependencies Added

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `com.github.ben-manes.caffeine:caffeine` | BOM-managed | In-memory template cache |
| `org.springframework.boot:spring-boot-starter-cache` | BOM-managed | Spring cache abstraction |
| `com.networknt:json-schema-validator` | 1.4.0 | Template and field validation |

### 9.2 Existing Dependencies Leveraged

| Dependency | Purpose in Transformer |
|-----------|----------------------|
| `spring-boot-starter-data-jpa` | TemplateHistoryEntity/Repository, JdbcTemplate (transitive) |
| `spring-boot-starter-web` | REST controller, Jackson serialization |
| `spring-boot-starter-validation` | @Validated request binding |
| `io.hypersistence:hypersistence-utils-hibernate-63` | @Type(JsonType.class) for JSONB columns |
| `org.postgresql:postgresql` | Database driver |
| `org.projectlombok:lombok` | Entity annotations (@Getter, @Setter, @Builder) |

### 9.3 Lombok Version Upgrade

Lombok was upgraded from 1.18.36 to **1.18.38** to resolve a `TypeTag :: UNKNOWN` compilation error caused by incompatibility with JDK 24.

---

## 10. Configuration

### 10.1 Application Properties

```yaml
# Added to application.yml
spring:
  cache:
    type: caffeine

transformer:
  template:
    dir: ${TRANSFORMER_TEMPLATE_DIR:classpath:templates}
    hot-reload: ${TRANSFORMER_HOT_RELOAD:true}
  cache:
    max-size: ${TRANSFORMER_CACHE_MAX_SIZE:500}
```

| Property | Default | Description |
|----------|---------|-------------|
| `transformer.template.dir` | `classpath:templates` | Directory containing `.transformer.json` files |
| `transformer.template.hot-reload` | `true` | Enable filesystem watch for template changes |
| `transformer.cache.max-size` | `500` | Maximum cached templates |

### 10.2 Environment Variable Overrides

For containerized deployments, all properties support environment variable overrides:

```bash
TRANSFORMER_TEMPLATE_DIR=/etc/transformer/templates
TRANSFORMER_HOT_RELOAD=true
TRANSFORMER_CACHE_MAX_SIZE=500
```

---

## 11. End-to-End Example

### 11.1 Write Request

```bash
curl -X POST http://localhost:3001/api/v1/submissions \
  -H "Content-Type: application/json" \
  -d '{
    "form_id": "employee-onboarding",
    "schema_version": 2,
    "submitted_by": "hr-admin-uuid",
    "form_data": {
      "personal": {
        "firstName": " Alex ",
        "lastName": " Rivera ",
        "email": "Alex.Rivera@example.com",
        "startDate": "2025-05-01"
      },
      "role": {
        "department": "Engineering",
        "title": "Senior Engineer",
        "salary": 145000,
        "remote": true
      },
      "equipment": {
        "preferences": { "os": "macOS", "monitor": "dual" }
      },
      "certifications": [
        { "name": "AWS Solutions Architect", "issuedBy": "Amazon", "expiryDate": "2027-03-15" },
        { "name": "Kubernetes CKA", "issuedBy": "CNCF", "expiryDate": "2026-11-01" }
      ]
    }
  }'
```

**What happens internally:**

1. Template `employee-onboarding` loaded (version 2)
2. `trimString` transform strips whitespace from firstName → "Alex", lastName → "Rivera"
3. `toLowerCase` transform normalizes email → "alex.rivera@example.com"
4. `toLocalDate` converts startDate string → `LocalDate(2025-05-01)` for DATE column
5. `toBigDecimal` converts salary → `BigDecimal(145000)`
6. `toBoolean` converts remote → `Boolean.TRUE`
7. `jsonStringify` serializes equipment.preferences → JSON string for JSONB column
8. Parent row INSERT into `form_employee_onboarding`
9. 2 child rows batch INSERT into `form_employee_onboarding_certifications`

**Response:**
```json
{
  "submission_id": 1,
  "form_id": "employee-onboarding",
  "version": 2,
  "created_at": "2026-04-06T14:32:00.000-04:00",
  "form_data": null
}
```

### 11.2 Read Request

```bash
curl http://localhost:3001/api/v1/submissions/1?form_id=employee-onboarding
```

**Response (round-trip fidelity):**
```json
{
  "submission_id": 1,
  "form_id": "employee-onboarding",
  "version": 2,
  "created_at": "2026-04-06T14:32:00.000-04:00",
  "form_data": {
    "personal": {
      "firstName": "Alex",
      "lastName": "Rivera",
      "email": "alex.rivera@example.com",
      "startDate": "2025-05-01"
    },
    "role": {
      "department": "Engineering",
      "title": "Senior Engineer",
      "salary": 145000,
      "remote": true
    },
    "equipment": {
      "preferences": { "os": "macOS", "monitor": "dual" }
    },
    "certifications": [
      { "name": "AWS Solutions Architect", "issuedBy": "Amazon", "expiryDate": "2027-03-15" },
      { "name": "Kubernetes CKA", "issuedBy": "CNCF", "expiryDate": "2026-11-01" }
    ]
  }
}
```

Note: `firstName` has been trimmed, `email` has been lowercased, and `startDate` has been round-tripped through `LocalDate → toIsoDateString`.

### 11.3 Validation Error Example

```bash
curl -X POST http://localhost:3001/api/v1/submissions \
  -H "Content-Type: application/json" \
  -d '{
    "form_id": "employee-onboarding",
    "schema_version": 2,
    "form_data": {
      "personal": { "email": "not-an-email" }
    }
  }'
```

**Response — HTTP 400:**
```json
{
  "error": "VALIDATION_FAILED",
  "violations": [
    { "json_path": "personal.firstName", "message": "Required field is missing" },
    { "json_path": "personal.lastName", "message": "Required field is missing" },
    { "json_path": "personal.email", "message": "must match format 'email'" },
    { "json_path": "personal.startDate", "message": "Required field is missing" },
    { "json_path": "role.department", "message": "Required field is missing" },
    { "json_path": "role.title", "message": "Required field is missing" },
    { "json_path": "role.remote", "message": "Required field is missing" }
  ]
}
```

---

## 12. Sample Transformer Templates

### 12.1 animal-research.transformer.json

A simple, flat form with no child tables or hooks. Demonstrates:
- Flat field mapping (no dot-notation nesting)
- JSONB transform for a string array (`animal_species`)
- Enum validation constraints
- All fields nullable

### 12.2 employee-onboarding.transformer.json

A complex form demonstrating all features. Demonstrates:
- Nested dot-notation paths (`personal.firstName`, `role.salary`, `equipment.preferences`)
- Multiple SQL types (TEXT, DATE, NUMERIC, BOOLEAN, JSONB)
- Multiple transforms (`trimString`, `toLowerCase`, `toLocalDate`, `toBigDecimal`, `toBoolean`, `jsonStringify`)
- Required fields (`nullable: false`) with default values
- Child table relation (`certifications` array)
- Hook configuration (preValidation, postInsert, postAssemble)
- Email format validation

---

## 13. Non-Functional Requirements

| Requirement | Implementation |
|-------------|----------------|
| **Transaction safety** | Write pipeline uses `@Transactional(isolation = REPEATABLE_READ)`. Parent and child writes are atomic. |
| **SQL injection prevention** | All table/column names validated via `IdentifierValidator` regex. All values use named parameters. |
| **Schema safety** | DdlManager is additive-only. No DROP COLUMN or DROP TABLE. |
| **Thread safety** | `ConcurrentHashMap` for template cache. Immutable Java records for template models. |
| **Graceful degradation** | Missing hook beans logged and skipped. Post-insert hook failures logged but do not roll back. |
| **Observability** | SLF4J structured logging at INFO/WARN/DEBUG levels. Spring Boot Actuator health/metrics endpoints. |
| **Configuration** | All settings overridable via environment variables for containerized deployments. |
| **Build** | Standard Maven fat JAR. Docker image from `eclipse-temurin:17-jre-alpine`. |

---

## 14. Open Items and Future Work

| ID | Item | Priority | Status |
|----|------|----------|--------|
| OQ-01 | Destructive column removal strategy | High | DdlManager implements NEVER DROP. Manual migration path documented. |
| OQ-02 | Git-based template sourcing | High | `TemplateLoader` interface supports future `GitTemplateLoader` implementation. |
| OQ-03 | Authentication model (JWT vs mTLS) | High | Deferred. `SecurityConfig` not yet implemented. |
| OQ-04 | Retry/DLQ for postInsert hooks | Medium | Current impl logs failures. `@TransactionalEventListener` approach for future. |
| OQ-05 | WebFlux reactive evaluation | Low | Not in scope. Standard Spring MVC. |
| OQ-06 | Bulk write API (CSV import) | Medium | Not in scope. Can be added via Spring Batch. |
| OQ-07 | S3 template loader | Low | Interface ready (`TemplateLoader`). `S3TemplateLoader` not yet implemented. |
| OQ-08 | Prometheus metrics for pipeline stages | Medium | Spring Boot Actuator present. Custom Micrometer counters/timers not yet added. |
| OQ-09 | Template version pinning on read | Medium | `TemplateHistoryRepository` populated. Read service does not yet query it for version-pinned reads. |
| OQ-10 | Comprehensive test suite | High | No tests exist. Unit + integration + contract tests needed. |

---

## 15. Testing Strategy (Planned)

| Layer | Tool | Scope |
|-------|------|-------|
| **Unit** | JUnit 5 + Mockito | PathResolver, TypeCoercionService, all 13 transforms, ValidationGate, FieldMapping logic |
| **Integration** | @SpringBootTest + Testcontainers (PostgreSQL) | DdlManager reconciliation, SqlExecutor CRUD, full write/read pipeline round-trip |
| **Contract** | MockMvc | REST endpoint request/response shapes, HTTP status codes, error response format |
| **Round-trip** | Custom assertion | POST formData → GET formData → assert deep equality per spec section 10.4 |

---

## 16. Deployment

### 16.1 Docker Compose (Development)

```bash
# Start PostgreSQL + service
docker-compose up -d

# PostgreSQL init.sql creates transformer_template_history table
# Service starts, loads templates, DdlManager creates per-form tables
```

### 16.2 Standalone JAR

```bash
cd service
mvn clean package -DskipTests
java -jar target/rjsf-form-service-1.0.0.jar \
  --spring.datasource.url=jdbc:postgresql://db:5432/rjsf_forms \
  --transformer.template.dir=/etc/transformer/templates
```

### 16.3 Docker Image

```dockerfile
# Multi-stage build (existing Dockerfile)
# Build: eclipse-temurin:17-jdk-alpine + mvn clean package
# Runtime: eclipse-temurin:17-jre-alpine
# Port: 3001
```

For production hot-reload, mount an external template directory:

```yaml
# docker-compose.yml addition
volumes:
  - ./transformer-templates:/etc/transformer/templates
environment:
  - TRANSFORMER_TEMPLATE_DIR=/etc/transformer/templates
```
