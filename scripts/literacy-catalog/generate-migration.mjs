/**
 * Generates Flyway migration V46 — upsert literacy cards (no DELETE).
 * Run: node scripts/literacy-catalog/build.mjs && node scripts/literacy-catalog/generate-migration.mjs
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '../..')
const CATALOG_PATH = join(ROOT, 'seed-review/info-cards-literacy-catalog.json')
const OUT_SQL = join(
  ROOT,
  'backend/finance-api/src/main/resources/db/migration/V46__seed_financial_literacy_info_cards.sql',
)

function sqlStr(value) {
  if (value == null || value === '') {
    return 'NULL'
  }
  return `'${String(value).replace(/'/g, "''")}'`
}

function sqlJsonb(value) {
  return `'${JSON.stringify(value).replace(/'/g, "''")}'::jsonb`
}

function blankToNull(value) {
  return value == null || String(value).trim() === '' ? null : String(value).trim()
}

function buildTranslations(card) {
  const out = {}
  for (const locale of ['tr', 'en', 'de']) {
    const src = card.translations[locale]
    out[locale] = {
      title: src.title,
      shortDescription: src.shortDescription,
      detailedDescription: src.detailedDescription ?? '',
      howToInterpret: blankToNull(src.howToInterpret),
      commonMistake: blankToNull(src.commonMistake),
      example: blankToNull(src.example),
      relatedTerms: src.relatedTerms ?? [],
    }
  }
  return out
}

function cardInsert(card) {
  const tr = card.translations.tr
  const translations = buildTranslations(card)
  const targetTerms = card.targetTerms?.length ? card.targetTerms : [tr.title]
  const pages = card.pages?.length ? card.pages : ['FINANCIAL_LITERACY']

  return `INSERT INTO info_cards (
    title, slug, status, card_type, difficulty, category,
    short_description, detailed_description, how_to_interpret,
    common_mistake, example_text, admin_only,
    target_terms, target_element_ids, target_instrument_symbols,
    pages, related_terms, translations
) VALUES (
    ${sqlStr(tr.title)},
    ${sqlStr(card.slug)},
    ${sqlStr(card.status ?? 'ACTIVE')},
    ${sqlStr(card.type)},
    ${sqlStr(card.difficulty)},
    ${sqlStr(card.category)},
    ${sqlStr(tr.shortDescription)},
    ${sqlStr(tr.detailedDescription ?? '')},
    ${sqlStr(blankToNull(tr.howToInterpret))},
    ${sqlStr(blankToNull(tr.commonMistake))},
    ${sqlStr(blankToNull(tr.example))},
    ${card.adminOnly ? 'TRUE' : 'FALSE'},
    ${sqlJsonb(targetTerms)},
    ${sqlJsonb(card.targetElementIds ?? [])},
    ${sqlJsonb(card.targetInstrumentSymbols ?? [])},
    ${sqlJsonb(pages)},
    ${sqlJsonb(tr.relatedTerms ?? [])},
    ${sqlJsonb(translations)}
)
ON CONFLICT (slug) DO UPDATE SET
    title = EXCLUDED.title,
    status = EXCLUDED.status,
    card_type = EXCLUDED.card_type,
    difficulty = EXCLUDED.difficulty,
    category = EXCLUDED.category,
    short_description = EXCLUDED.short_description,
    detailed_description = EXCLUDED.detailed_description,
    how_to_interpret = EXCLUDED.how_to_interpret,
    common_mistake = EXCLUDED.common_mistake,
    example_text = EXCLUDED.example_text,
    admin_only = EXCLUDED.admin_only,
    related_terms = EXCLUDED.related_terms,
    translations = EXCLUDED.translations,
    updated_at = NOW(),
    pages = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.pages || EXCLUDED.pages) AS elem
        ) merged_pages
    ),
    target_terms = (
        SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
        FROM (
            SELECT jsonb_array_elements_text(info_cards.target_terms || EXCLUDED.target_terms) AS elem
        ) merged_terms
    ),
    target_element_ids = CASE
        WHEN EXCLUDED.target_element_ids IS NULL
            OR EXCLUDED.target_element_ids = '[]'::jsonb
        THEN info_cards.target_element_ids
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_element_ids || EXCLUDED.target_element_ids
                ) AS elem
            ) merged_elements
        )
    END,
    target_instrument_symbols = CASE
        WHEN EXCLUDED.target_instrument_symbols IS NULL
            OR EXCLUDED.target_instrument_symbols = '[]'::jsonb
        THEN info_cards.target_instrument_symbols
        ELSE (
            SELECT COALESCE(jsonb_agg(DISTINCT elem ORDER BY elem), '[]'::jsonb)
            FROM (
                SELECT jsonb_array_elements_text(
                    info_cards.target_instrument_symbols || EXCLUDED.target_instrument_symbols
                ) AS elem
            ) merged_instruments
        )
    END;`
}

const catalog = JSON.parse(readFileSync(CATALOG_PATH, 'utf8'))
const header = `-- Finansal Okuryazarlık genel bilgi kartları (${catalog.cards.length} kart)
-- Güvenli upsert: hiçbir satır SİLİNMEZ.
-- ON CONFLICT (slug): içerik güncellenir; pages/target_terms birleştirilir;
-- target_element_ids / target_instrument_symbols yalnızca yeni değer varsa birleştirilir (boş [] mevcut bağları silmez).
-- Oluşturulma: node scripts/literacy-catalog/generate-migration.mjs

`

const body = catalog.cards.map(cardInsert).join('\n\n')
writeFileSync(OUT_SQL, header + body + '\n', 'utf8')
console.log(`Wrote migration → ${OUT_SQL}`)
console.log(`Cards: ${catalog.cards.length}`)
