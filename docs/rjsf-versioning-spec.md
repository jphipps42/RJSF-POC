# RJSF Form Versioning — Technical Specification

**Topic:** RJSF Form Versioning in a Relational Database  
**Scope:** Schema evolution, submission pinning, data migration, and RJSF integration patterns

---

## Table of Contents

1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Database Schema Design](#database-schema-design)
4. [Version Lifecycle Strategy](#version-lifecycle-strategy)
5. [Loading the Right Schema for Rendering](#loading-the-right-schema-for-rendering)
6. [Data Migration Between Versions](#data-migration-between-versions)
7. [RJSF Integration Pattern](#rjsf-integration-pattern)
8. [Key Design Decisions](#key-design-decisions)
9. [Gotchas and Edge Cases](#gotchas-and-edge-cases)

---

## Overview

Versioning an RJSF form implementation requires managing schema evolution across two distinct concerns: the **form definition** (JSON Schema + UISchema) and the **form data** (submissions). Each submission must be permanently pinned to the schema version that was active at the time of submission, enabling both accurate audit history and forward-migration for edits.

---

## Core Concepts

Two separate artifacts require versioning:

- **The schema** — the RJSF JSON Schema + UISchema that defines the form's structure and validation rules
- **The data** — form submissions tied to a specific schema version at the time of submission

---

## Database Schema Design

### `form_schema_versions`

Stores each published version of the form definition.

```sql
CREATE TABLE form_schema_versions (
  id              SERIAL PRIMARY KEY,
  form_id         UUID NOT NULL,           -- logical form identity
  version         INTEGER NOT NULL,
  schema          JSONB NOT NULL,          -- RJSF JSON Schema
  ui_schema       JSONB,                   -- RJSF uiSchema
  change_notes    TEXT,
  is_current      BOOLEAN DEFAULT false,
  created_at      TIMESTAMPTZ DEFAULT NOW(),
  created_by      UUID,
  UNIQUE (form_id, version)
);
```

### `form_submissions`

Form submissions, each pinned to the schema version used at submit time.

```sql
CREATE TABLE form_submissions (
  id                     SERIAL PRIMARY KEY,
  form_id                UUID NOT NULL,
  schema_version_id      INTEGER REFERENCES form_schema_versions(id),
  schema_version         INTEGER NOT NULL,  -- denormalized for fast queries
  data                   JSONB NOT NULL,    -- raw submitted formData
  submitted_at           TIMESTAMPTZ DEFAULT NOW(),
  submitted_by           UUID
);
```

### `schema_migrations`

Tracks field-level transformation rules between consecutive versions.

```sql
CREATE TABLE schema_migrations (
  id               SERIAL PRIMARY KEY,
  form_id          UUID NOT NULL,
  from_version     INTEGER NOT NULL,
  to_version       INTEGER NOT NULL,
  migration_script JSONB,   -- transformation rules (see Data Migration section)
  created_at       TIMESTAMPTZ DEFAULT NOW()
);
```

---

## Version Lifecycle Strategy

### Publishing a New Version

```typescript
async function publishNewSchemaVersion(
  formId: string,
  newSchema: RJSFSchema,
  newUiSchema: UiSchema,
  notes: string
): Promise<number> {
  return db.transaction(async (trx) => {
    const current = await trx('form_schema_versions')
      .where({ form_id: formId, is_current: true })
      .first();

    const nextVersion = current ? current.version + 1 : 1;

    // Demote previous current
    if (current) {
      await trx('form_schema_versions')
        .where({ id: current.id })
        .update({ is_current: false });
    }

    const [newRow] = await trx('form_schema_versions').insert({
      form_id: formId,
      version: nextVersion,
      schema: JSON.stringify(newSchema),
      ui_schema: JSON.stringify(newUiSchema),
      change_notes: notes,
      is_current: true,
    }).returning('*');

    return newRow.version;
  });
}
```

---

## Loading the Right Schema for Rendering

### Audit View — Render with Original Schema

When displaying an existing submission in read-only mode, always render using the schema version it was created with.

```typescript
async function getSubmissionWithSchema(submissionId: number) {
  const submission = await db('form_submissions as s')
    .join('form_schema_versions as v', 'v.id', 's.schema_version_id')
    .where('s.id', submissionId)
    .select('s.data', 'v.schema', 'v.ui_schema', 'v.version')
    .first();

  return {
    formData: submission.data,
    schema: submission.schema,
    uiSchema: submission.ui_schema,
    schemaVersion: submission.version,
  };
}
```

### New Submission — Render with Current Schema

```typescript
async function getCurrentSchema(formId: string) {
  return db('form_schema_versions')
    .where({ form_id: formId, is_current: true })
    .first();
}
```

---

## Data Migration Between Versions

### Migration Rule Types

```typescript
type MigrationRule =
  | { op: 'rename'; from: string; to: string }
  | { op: 'set_default'; field: string; value: unknown }
  | { op: 'drop'; field: string }
  | { op: 'transform'; field: string; fn: string }; // named transform
```

### Applying Migration Rules

```typescript
function migrateFormData(
  data: Record<string, unknown>,
  rules: MigrationRule[]
): Record<string, unknown> {
  let result = { ...data };

  for (const rule of rules) {
    if (rule.op === 'rename') {
      result[rule.to] = result[rule.from];
      delete result[rule.from];
    } else if (rule.op === 'set_default' && !(rule.field in result)) {
      result[rule.field] = rule.value;
    } else if (rule.op === 'drop') {
      delete result[rule.field];
    }
  }

  return result;
}
```

### Migrating an Old Submission Forward to Current Version

Used when a user wants to **edit** an existing submission — migrate the data forward to the current schema before rendering the form.

```typescript
async function migrateToCurrentVersion(submissionId: number) {
  const { data, schemaVersion, formId } = await getSubmissionWithSchema(submissionId);
  const current = await getCurrentSchema(formId);

  // Load all migration steps between old version and current
  const migrations = await db('schema_migrations')
    .where('form_id', formId)
    .whereBetween('from_version', [schemaVersion, current.version - 1])
    .orderBy('from_version', 'asc');

  let migratedData = data;
  for (const migration of migrations) {
    migratedData = migrateFormData(migratedData, migration.migration_script);
  }

  return { formData: migratedData, schema: current.schema, uiSchema: current.ui_schema };
}
```

---

## RJSF Integration Pattern

```tsx
function VersionedForm({ formId, submissionId }: Props) {
  const [formConfig, setFormConfig] = useState<{
    schema: RJSFSchema;
    uiSchema: UiSchema;
    formData: unknown;
    schemaVersion: number;
  } | null>(null);

  useEffect(() => {
    const load = submissionId
      ? migrateToCurrentVersion(submissionId)    // editing existing
      : getCurrentSchema(formId).then(s => ({    // new submission
          schema: s.schema,
          uiSchema: s.ui_schema,
          formData: {},
          schemaVersion: s.version,
        }));

    load.then(setFormConfig);
  }, [formId, submissionId]);

  const handleSubmit = ({ formData }) => {
    saveSubmission({
      formId,
      schemaVersionId: formConfig.schemaVersion, // always pin to current
      data: formData,
    });
  };

  if (!formConfig) return <Spinner />;

  return (
    <Form
      schema={formConfig.schema}
      uiSchema={formConfig.uiSchema}
      formData={formConfig.formData}
      onSubmit={handleSubmit}
    />
  );
}
```

---

## Key Design Decisions

| Concern | Recommendation |
|---|---|
| Schema storage | JSONB in Postgres — queryable and flexible |
| Submission pinning | Always store `schema_version_id` at submit time |
| Audit / history views | Render with the original schema version, never the current |
| Editing old submissions | Migrate data forward to current schema before re-render |
| Breaking changes | Require an explicit migration rule; block publish if none provided |
| Rollback | Mark a prior version `is_current = true`; no data deletion needed |

---

## Gotchas and Edge Cases

### `$ref` and `$defs` Resolution

If your schemas use JSON Schema `$ref` references, store the **fully-dereferenced** schema at publish time using [`json-schema-ref-parser`](https://github.com/APIDevTools/json-schema-ref-parser). This ensures each version is entirely self-contained and can be rendered independently without needing to resolve external references.

```typescript
import $RefParser from '@apidevtools/json-schema-ref-parser';

const dereferenced = await $RefParser.dereference(schema);
// Store `dereferenced`, not the original `schema`
```

### Validation on Migrated Data

When migrating old data forward, always run it through a validator before rendering. This surfaces any gaps in migration rules early and provides a clear error to the user.

```typescript
import Ajv from 'ajv';

const ajv = new Ajv();
const valid = ajv.validate(currentSchema, migratedData);
if (!valid) {
  console.warn('Migration gaps detected:', ajv.errors);
  // Surface to UI rather than silently rendering invalid data
}
```

### Draft Submissions

Drafts should pin to the schema version active **when the draft was created**, not when it is finally submitted. If the schema advances between draft creation and final submission, apply the same migration-forward pattern before re-rendering the draft for completion.

### Avoiding Implicit Breaking Changes

Track the following schema changes as breaking (require a migration rule):
- Renaming a field
- Removing a required field
- Changing a field's type
- Restructuring nested objects

Additive changes (new optional fields, new enum values, new UI hints) are generally safe without a migration rule.
