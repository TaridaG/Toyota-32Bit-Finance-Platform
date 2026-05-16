CREATE TABLE info_cards (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(500) NOT NULL,
    slug            VARCHAR(500) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    card_type       VARCHAR(50) NOT NULL,
    difficulty      VARCHAR(20) NOT NULL,
    category        VARCHAR(50) NOT NULL,
    short_description TEXT NOT NULL,
    detailed_description TEXT NOT NULL,
    how_to_interpret TEXT,
    common_mistake  TEXT,
    example_text    TEXT,
    admin_only      BOOLEAN NOT NULL DEFAULT FALSE,
    target_terms    JSONB NOT NULL DEFAULT '[]'::jsonb,
    target_element_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    pages           JSONB NOT NULL DEFAULT '[]'::jsonb,
    related_terms   JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_info_cards_slug UNIQUE (slug),
    CONSTRAINT chk_info_cards_status CHECK (status IN ('ACTIVE', 'PASSIVE'))
);

CREATE INDEX idx_info_cards_status ON info_cards (status);
CREATE INDEX idx_info_cards_updated_at ON info_cards (updated_at DESC);
CREATE INDEX idx_info_cards_pages_gin ON info_cards USING GIN (pages);
