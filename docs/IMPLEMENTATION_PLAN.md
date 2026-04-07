# RJSF Form Transformer Microservice — Technical Implementation Plan

**Based on:** `rjsf-transformer-microservice-spec-java17.docx` v2.0
**Target:** Spring Boot 3.5 / Java 17 / PostgreSQL 15+
**Date:** 2026-04-04

---

## Current State Assessment

The existing `service/` module (`com.egs.rjsf`) is a Spring Boot 3.4.4 / Java 17 application that stores form submissions as **opaque JSONB blobs** in a `form_submissions` table. It already has:

- Award/submission/personnel/linked-file CRUD
- Schema versioning with a declarative migration engine (`MigrationEngine.java`)
- Form configuration management (JSON Schema + UI Schema stored as JSONB)
- PostgreSQL with JPA/Hibernate, running on port 3001
- Docker + docker-compose infrastructure

**The spec asks us to add** a parallel, template-driven engine that flattens RJSF `formData` into **discrete typed relational columns** — one table per form — with bidirectional JSON-to-relational transformation. This is a new subsystem that coexists with the existing JSONB-based submission storage.

---

## Implementation Phases

### Phase 1 — Foundation: Models, Configuration, Template Registry
**Estimated scope:** ~12 files | **Risk:** Low

#### 1.1 Upgrade Spring Boot to 3.5.x
- Update `pom.xml` parent from `3.4.4` → `3.5.0`
- Verify all existing dependencies resolve cleanly
- Run existing tests to confirm no regressions

#### 1.2 Add New Dependencies to `pom.xml`
```
+ com.networknt:json-schema-validator:1.4.0     (template & field validation)
+ com.github.ben-manes.caffeine:caffeine        (template cache)
+ spring-boot-starter-cache                      (cache abstraction)
+ org.flywaydb:flyway-core                       (DDL baseline migrations)
+ org.flywaydb:flyway-database-postgresql
+ org.testcontainers:postgresql                  (integration tests)
```
Note: `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server` are deferred to Phase 5 (security is an open item per OQ-03).

#### 1.3 Create Java Record Models
**Package:** `com.egs.rjsf.transformer.model`

| File | Description |
|------|-------------|
| `TransformerTemplate.java` | Top-level record: formId, version, tableName, fields, relations, hooks |
| `FieldMapping.java` | Record: jsonPath, column, sqlType, nullable, defaultValue, transform, validation, tags |
| `RelationMapping.java` | Record: jsonPath, childTable, parentKey, orderColumn, fields |
| `TransformConfig.java` | Record: toDb, fromDb |
| `SchemaVersionConfig.java` | Record: column, sqlType |
| `AuditColumnConfig.java` | Record: createdAt, updatedAt, submittedBy |
| `HookConfig.java` | Record: preValidation, preInsert, postInsert, postQuery, postAssemble (all `List<String>`) |

All records use Jackson annotations for deserialization from transformer JSON files. Use `@JsonProperty` defaults and `@JsonInclude(NON_NULL)` where appropriate.

#### 1.4 Template Loader Interface and FileSystem Implementation
**Package:** `com.egs.rjsf.transformer.registry`

| File | Description |
|------|-------------|
| `TemplateLoader.java` | Interface: `load(formId)`, `list()`, `watch(Consumer<String>)` |
| `FileSystemTemplateLoader.java` | Loads from `transformer.template.dir`, uses `WatchService` for hot reload |

- Template directory configured via `transformer.template.dir` property (default: `classpath:templates`)
- Files named `{formId}.transformer.json`
- `WatchService` daemon thread in a `SmartLifecycle` bean for hot reload (controlled by `transformer.template.hot-reload` property)

#### 1.5 Template Registry with Caffeine Cache
**File:** `TemplateRegistry.java`

- Wraps `Caffeine LoadingCache<String, TransformerTemplate>` keyed by formId
- Max size configurable via `transformer.cache.max-size` (default 500)
- No automatic expiry — invalidated on file-change events or via management endpoint
- On cache miss, delegates to `TemplateLoader.load(formId)`
- On hot-reload event: re-parse, validate, replace cache entry, trigger DDL reconciliation

#### 1.6 Template Validator
**File:** `TemplateValidator.java`

- Validates loaded template JSON against a meta-schema using NetworkNT
- Invalid templates rejected; previous cached version stays active
- Logs ERROR + increments Micrometer counter `transformer.template.validation.failures`

#### 1.7 Configuration Properties
**File:** `TransformerProperties.java` — `@ConfigurationProperties(prefix = "transformer")`

Add to `application.yml`:
```yaml
transformer:
  template:
    dir: ${TRANSFORMER_TEMPLATE_DIR:classpath:templates}
    loader: filesystem
    hot-reload: true
  cache:
    max-size: 500
```

#### 1.8 Deliverables Checklist — Phase 1
- [ ] Spring Boot 3.5 upgrade verified
- [ ] All 7 model records compile and deserialize from sample JSON
- [ ] `FileSystemTemplateLoader` loads templates from disk
- [ ] `TemplateRegistry` caches and serves templates
- [ ] `TemplateValidator` rejects malformed templates
- [ ] Unit tests for deserialization and validation

---

### Phase 2 — Transform Engine: Type Coercion, Path Resolution, Field Mapping
**Estimated scope:** ~8 files | **Risk:** Medium (core logic)

#### 2.1 Path Resolver
**File:** `com.egs.rjsf.transformer.engine.PathResolver.java`

- Deep-get and deep-set on `Map<String, Object>` using dot-notation paths
- `get("patientInfo.dateOfBirth", formData)` → `Optional<Object>`
- `set("patientInfo.dateOfBirth", value, formData)` → creates intermediate maps as needed
- Handles both nested objects and array-element access

#### 2.2 Transform Registry and Built-In Transforms
**Package:** `com.egs.rjsf.transformer.transform`

| File | Description |
|------|-------------|
| `TransformFunction.java` | `@interface` annotation + `ToDbFunction` / `FromDbFunction` functional interfaces |
| `TransformRegistry.java` | `@Component` that discovers `@TransformFunction`-annotated beans via classpath scanning |
| `BuiltInTransforms.java` | All 14 built-in transforms as inner `@Component` classes |

**Built-in transforms to implement:**
`toLocalDate`, `toIsoDateString`, `toInstant`, `fromInstant`, `jsonStringify`, `jsonParse`, `trimString`, `toLowerCase`, `toBoolean`, `toBigDecimal`, `toInteger`, `maskLast4`, `toUUID`

#### 2.3 Type Coercion Service
**File:** `com.egs.rjsf.transformer.engine.TypeCoercionService.java`

- Converts Java values to the target `sqlType` declared in the FieldMapping
- Handles: `TEXT`, `INTEGER`, `NUMERIC`, `BOOLEAN`, `DATE`, `TIMESTAMPTZ`, `VARCHAR(n)`, `JSONB`, `UUID`
- For absent optional fields: returns `defaultValue` or `null`

#### 2.4 Field Mapper
**File:** `com.egs.rjsf.transformer.engine.FieldMapper.java`

- **Write direction:** For each `FieldMapping`, extract value via `PathResolver`, apply `toDb` transform, coerce type, populate column-value map
- **Read direction:** For each `FieldMapping`, get column value, apply `fromDb` transform, set at jsonPath in output map

#### 2.5 Transformer Engine
**File:** `com.egs.rjsf.transformer.engine.TransformerEngine.java`

- `write(WriteRequest)` → orchestrates the full write pipeline (field mapping only; hooks and SQL in later phases)
- `read(ReadRequest)` → orchestrates the full read pipeline (field assembly only)
- Pure mapping logic; no database interaction in this phase

#### 2.6 Deliverables Checklist — Phase 2
- [ ] `PathResolver` handles nested paths including edge cases (missing intermediates, null values)
- [ ] All 14 built-in transforms implemented with unit tests
- [ ] `TransformRegistry` discovers custom transforms
- [ ] `TypeCoercionService` handles all 9 SQL types
- [ ] `FieldMapper` produces correct column-value maps from formData and vice versa
- [ ] End-to-end unit test: formData → column map → reconstructed formData matches original

---

### Phase 3 — DDL Management and SQL Execution
**Estimated scope:** ~5 files | **Risk:** High (schema mutation, data integrity)

#### 3.1 Flyway Baseline Migration
**File:** `src/main/resources/db/migration/V3__create_transformer_template_history.sql`

```sql
CREATE TABLE transformer_template_history (
  id            BIGSERIAL PRIMARY KEY,
  form_id       TEXT    NOT NULL,
  version       INTEGER NOT NULL,
  template_json JSONB   NOT NULL,
  loaded_at     TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (form_id, version)
);
```

Note: Numbered V3 to follow existing migration files (001, 002).

#### 3.2 Template History Repository
**File:** `com.egs.rjsf.transformer.ddl.TemplateHistoryRepository.java`

- Spring Data JPA repository for `transformer_template_history`
- `findByFormIdAndVersion(formId, version)` for historical reads
- Called by `TemplateRegistry` on every successful template load

#### 3.3 DDL Manager
**File:** `com.egs.rjsf.transformer.ddl.DdlManager.java`

- `@Component` executed via `ApplicationRunner` on startup
- `reconcile(TransformerTemplate)` method:
  - If table doesn't exist → `CREATE TABLE` with all fields, audit columns, system columns
  - If table exists → `ALTER TABLE ADD COLUMN` for missing columns (always nullable)
  - **Never DROP** — logs WARN for deprecated columns
- Child table reconciliation for `RelationMapping` entries
- Uses `DatabaseMetaData` to inspect existing schema
- Each reconciliation runs in `@Transactional(propagation = REQUIRES_NEW)`

**DDL Safety Rules (enforced in code):**
- No `DROP COLUMN` or `DROP TABLE` ever
- New columns added via ALTER are always nullable
- NOT NULL enforced at application layer (Validation Gate), not DB layer for post-creation columns

#### 3.4 SQL Executor
**File:** `com.egs.rjsf.transformer.db.SqlExecutor.java`

- Wraps `NamedParameterJdbcTemplate`
- `insert(tableName, columnValues)` → `INSERT ... RETURNING submission_id` with `GeneratedKeyHolder`
- `batchInsertChildren(childTable, rows)` → `JdbcTemplate.batchUpdate()` for relation arrays
- `selectById(tableName, columns, submissionId)` → `queryForMap()`
- `selectChildren(childTable, columns, submissionId, orderColumn)` → `queryForList()` ordered by `item_index`

#### 3.5 JDBC Configuration
**File:** `com.egs.rjsf.transformer.config.JdbcConfig.java`

- Defines `NamedParameterJdbcTemplate` bean if not already present
- Configures `DataSource` properties (reuses existing datasource config)

#### 3.6 Deliverables Checklist — Phase 3
- [ ] Flyway migration creates `transformer_template_history` table
- [ ] `DdlManager` creates tables from template on startup
- [ ] `DdlManager` adds missing columns on template update without dropping existing ones
- [ ] `SqlExecutor` inserts and queries parent + child tables correctly
- [ ] Integration test with Testcontainers: template load → DDL → insert → select round-trip

---

### Phase 4 — Validation, Hooks, and REST API
**Estimated scope:** ~10 files | **Risk:** Medium

#### 4.1 Validation Gate
**File:** `com.egs.rjsf.transformer.validation.ValidationGate.java`

- Iterates each field's `validation` JSON Schema fragment
- Uses NetworkNT `JsonSchemaFactory` to validate individual field values
- Fields with `nullable: false` that are absent → violation
- Collects **all** violations before throwing (not fail-fast)
- Returns structured error: `{ "error": "VALIDATION_FAILED", "violations": [...] }`

#### 4.2 Hook System
**Package:** `com.egs.rjsf.transformer.hooks`

| File | Description |
|------|-------------|
| `WriteHook.java` | Functional interface for write-path hooks |
| `ReadHook.java` | Functional interface for read-path hooks |
| `HookRegistry.java` | Resolves hook beans by name from `ApplicationContext` |

**Hook points (per spec section 3.5):**

| Hook Point | Path | Signature |
|------------|------|-----------|
| preValidation | Write | `Map<String,Object> apply(Map<String,Object> formData)` |
| preInsert | Write | `Map<String,Object> apply(Map<String,Object> columnValues)` |
| postInsert | Write | `void accept(Long submissionId, Map<String,Object> formData)` |
| postQuery | Read | `Map<String,Object> apply(Map<String,Object> dbRow)` |
| postAssemble | Read | `Map<String,Object> apply(Map<String,Object> formData)` |

#### 4.3 Submission Write Service
**File:** `com.egs.rjsf.transformer.service.SubmissionWriteService.java`

Orchestrates the 7-stage write pipeline:
1. Template Resolution (via `TemplateRegistry`)
2. Pre-Validation Hooks
3. Validation Gate
4. Field Mapping (via `TransformerEngine`)
5. Pre-Insert Hooks
6. Transactional Write (`@Transactional(isolation = REPEATABLE_READ)`)
7. Post-Insert Hooks (outside transaction)

#### 4.4 Submission Read Service
**File:** `com.egs.rjsf.transformer.service.SubmissionReadService.java`

Orchestrates the 7-stage read pipeline:
1. Template Resolution (with optional version pinning via `TemplateHistoryRepository`)
2. Primary Row Query
3. Child Table Queries
4. Post-Query Hooks
5. Field Assembly (via `TransformerEngine`)
6. Post-Assembly Hooks
7. Response Envelope

#### 4.5 REST Controller
**File:** `com.egs.rjsf.transformer.api.TransformerSubmissionController.java`

```
POST /api/v1/submissions      → SubmissionWriteService.write()
GET  /api/v1/submissions/{id} → SubmissionReadService.read()
```

Note: Uses the `/api/v1/` prefix to coexist with existing `/api/` endpoints.

#### 4.6 DTOs
**Package:** `com.egs.rjsf.transformer.api.dto`

| File | Description |
|------|-------------|
| `TransformerSubmissionRequest.java` | Record: formId, schemaVersion, submittedBy, formData |
| `TransformerSubmissionResponse.java` | Record: submissionId, formId, version, createdAt, formData (for reads) |

#### 4.7 Exception Handling
**File:** `com.egs.rjsf.transformer.api.exception.TransformerExceptionHandler.java`

| Exception | HTTP | Error Code |
|-----------|------|------------|
| `TemplateNotFoundException` | 400 | TEMPLATE_NOT_FOUND |
| `ValidationException` | 400 | VALIDATION_FAILED |
| `VersionMismatchException` | 400 | VERSION_MISMATCH |
| `DuplicateSubmissionException` | 409 | DUPLICATE_SUBMISSION |
| `SubmissionNotFoundException` | 404 | SUBMISSION_NOT_FOUND |
| `DataAccessException` | 500 | DB_ERROR |

#### 4.8 Deliverables Checklist — Phase 4
- [ ] Validation Gate catches all field violations and returns structured errors
- [ ] Hook system resolves and invokes beans by name in declared order
- [ ] Write pipeline: full 7-stage flow works end-to-end
- [ ] Read pipeline: full 7-stage flow reconstructs original formData
- [ ] REST endpoints return correct HTTP status codes and error bodies
- [ ] Integration test: POST submission → GET submission → round-trip fidelity verified

---

### Phase 5 — Observability, Security, and Production Hardening
**Estimated scope:** ~5 files | **Risk:** Low-Medium

#### 5.1 Actuator and Metrics
- Add Micrometer counters/timers for: template load, validation, SQL write, SQL read
- Expose Prometheus metrics at `/actuator/prometheus`
- Custom `/actuator/template-cache` management endpoint for cache inspection/invalidation

#### 5.2 Structured Logging
- Configure Logback ECS JSON appender for structured logs
- Audit log entries for every write and read operation via SLF4J

#### 5.3 Security (pending OQ-03 resolution)
- Add `spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server`
- Configure `SecurityConfig.java` with JWT bearer or mTLS (based on team decision)
- Wire PII tag-based masking in `postQuery` hooks using `SecurityContextHolder`

#### 5.4 Deliverables Checklist — Phase 5
- [ ] Prometheus metrics endpoint returns transformer-specific counters
- [ ] Structured JSON logs emitted for all pipeline stages
- [ ] Security configuration in place (JWT or mTLS)
- [ ] Cache management endpoint functional

---

### Phase 6 — Sample Templates and End-to-End Validation
**Estimated scope:** ~4 files | **Risk:** Low

#### 6.1 Create Sample Transformer Templates
Based on the existing form configurations in `init.sql`:

| Template File | Form |
|--------------|------|
| `animal-research.transformer.json` | Animal Research Review Requirements |
| `employee-onboarding.transformer.json` | Example from spec section 10 |

#### 6.2 Sample Hook Implementations
- `normalizePhoneNumbersHook` — preValidation example
- `emitAuditEventHook` — postInsert example using Spring `ApplicationEvent`
- `maskPiiFieldsHook` — postQuery example

#### 6.3 Docker Compose Update
- Update `docker-compose.yml` to ensure PostgreSQL 15+ is used
- Add Flyway migration volume mount if needed

#### 6.4 Deliverables Checklist — Phase 6
- [ ] Sample templates load and generate correct DDL
- [ ] Full round-trip test with sample data passes
- [ ] Docker compose builds and runs successfully

---

## New Package Structure

All new code lives under `com.egs.rjsf.transformer` to cleanly separate from existing `com.egs.rjsf` code:

```
service/src/main/java/com/egs/rjsf/transformer/
├── api/
│   ├── TransformerSubmissionController.java
│   ├── dto/
│   │   ├── TransformerSubmissionRequest.java
│   │   └── TransformerSubmissionResponse.java
│   └── exception/
│       ├── TransformerExceptionHandler.java
│       ├── TemplateNotFoundException.java
│       ├── ValidationException.java
│       ├── VersionMismatchException.java
│       ├── DuplicateSubmissionException.java
│       └── SubmissionNotFoundException.java
├── engine/
│   ├── TransformerEngine.java
│   ├── FieldMapper.java
│   ├── PathResolver.java
│   └── TypeCoercionService.java
├── registry/
│   ├── TemplateRegistry.java
│   ├── TemplateLoader.java
│   ├── FileSystemTemplateLoader.java
│   └── TemplateValidator.java
├── ddl/
│   ├── DdlManager.java
│   └── TemplateHistoryRepository.java
├── db/
│   └── SqlExecutor.java
├── transform/
│   ├── TransformRegistry.java
│   ├── TransformFunction.java
│   ├── ToDbFunction.java
│   ├── FromDbFunction.java
│   └── BuiltInTransforms.java
├── hooks/
│   ├── WriteHook.java
│   ├── ReadHook.java
│   └── HookRegistry.java
├── validation/
│   └── ValidationGate.java
├── model/
│   ├── TransformerTemplate.java
│   ├── FieldMapping.java
│   ├── RelationMapping.java
│   ├── TransformConfig.java
│   ├── SchemaVersionConfig.java
│   ├── AuditColumnConfig.java
│   └── HookConfig.java
├── service/
│   ├── SubmissionWriteService.java
│   └── SubmissionReadService.java
└── config/
    ├── TransformerProperties.java
    └── JdbcConfig.java
```

**Total new files: ~40**

---

## Implementation Order and Dependencies

```
Phase 1 ──→ Phase 2 ──→ Phase 3 ──→ Phase 4 ──→ Phase 5
  (models,     (transforms,  (DDL,        (validation,  (metrics,
   registry)    path logic)   SQL exec)    hooks, API)   security)
                                               │
                                               ▼
                                           Phase 6
                                          (samples,
                                           e2e tests)
```

Each phase builds on the prior one. Phases 5 and 6 can partially overlap.

---

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Package namespace | `com.egs.rjsf.transformer.*` | Cleanly separates from existing `com.egs.rjsf.*` code; no refactoring of existing classes |
| API prefix | `/api/v1/submissions` | Avoids collision with existing `/api/form-submissions` endpoints |
| Spring Boot version | Upgrade to 3.5.0 | Per spec requirement; also picks up latest Caffeine and Micrometer support |
| DDL approach | `DdlManager` + Flyway hybrid | DdlManager for dynamic per-form tables; Flyway for system tables only |
| S3 loader | Deferred | Spec declares it optional; `TemplateLoader` interface enables future addition |
| Security | Deferred to Phase 5 | Per OQ-03, auth model not yet decided; interface-segregated so it can be plugged in |
| Reactive (WebFlux) | Not implemented | Per OQ-05, low priority; standard MVC is sufficient for Java 17 |

---

## Open Items from Spec (Tracked)

| ID | Item | Impact on Implementation |
|----|------|--------------------------|
| OQ-01 | Destructive column removal strategy | DdlManager implements NEVER DROP; manual Flyway path documented |
| OQ-02 | Git-based template sourcing | TemplateLoader interface supports future GitTemplateLoader |
| OQ-03 | Auth model (JWT vs mTLS) | SecurityConfig deferred; skeleton prepared in Phase 5 |
| OQ-04 | Retry/DLQ for postInsert hooks | Phase 4 implements fire-and-forget; retry is a follow-up |
| OQ-05 | WebFlux evaluation | Not in scope; MVC-only |
| OQ-06 | Bulk write API | Not in scope; can be added later via Spring Batch |

---

## Testing Strategy

| Layer | Tool | Coverage |
|-------|------|----------|
| Unit | JUnit 5 + Mockito | All transforms, PathResolver, TypeCoercionService, FieldMapper, ValidationGate |
| Integration | @SpringBootTest + Testcontainers (PostgreSQL) | DdlManager reconciliation, SqlExecutor CRUD, full write/read pipeline |
| Contract | MockMvc | REST endpoint request/response shapes, error codes |
| Round-trip | Custom | POST formData → GET formData → assert deep equality (spec section 10.4) |
