-- Transformer template history: stores each loaded template version for version pinning on reads
CREATE TABLE IF NOT EXISTS transformer_template_history (
    id              BIGSERIAL PRIMARY KEY,
    form_id         TEXT        NOT NULL,
    version         INTEGER     NOT NULL,
    template_json   JSONB       NOT NULL,
    loaded_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (form_id, version)
);

CREATE INDEX IF NOT EXISTS idx_tth_form_id ON transformer_template_history(form_id);
CREATE INDEX IF NOT EXISTS idx_tth_form_version ON transformer_template_history(form_id, version);
