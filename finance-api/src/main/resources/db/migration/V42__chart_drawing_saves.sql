CREATE TABLE chart_drawing_saves (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    instrument_id   BIGINT       REFERENCES instruments (id),
    asset_key       VARCHAR(64)  NOT NULL,
    asset_symbol    VARCHAR(32)  NOT NULL,
    asset_type      VARCHAR(24),
    name            VARCHAR(120) NOT NULL,
    drawings_json   TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chart_drawing_saves_user_asset_created
    ON chart_drawing_saves (user_id, asset_key, created_at DESC);
