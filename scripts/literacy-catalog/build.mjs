/**
 * Builds seed-review/info-cards-literacy-catalog.json + catalog-review.html
 * Run: node scripts/literacy-catalog/build.mjs
 * Does NOT write to the database.
 */
import { writeFileSync, mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { EXISTING_TR } from './data/existing-tr.mjs'
import { I18N_EN } from './data/i18n-en.mjs'
import { I18N_DE } from './data/i18n-de.mjs'
import { NEW_CARDS } from './data/new-cards.mjs'

const __dirname = dirname(fileURLToPath(import.meta.url))
const ROOT = join(__dirname, '../..')
const OUT_DIR = join(ROOT, 'seed-review')

function pickLocale(tr, map, locale) {
  const row = map[tr.slug]
  if (!row?.[locale]) {
    throw new Error(`Missing ${locale} translation for slug=${tr.slug}`)
  }
  return row[locale]
}

function toCard(tr, en, de, extra = {}) {
  const relatedTr = tr.relatedTerms ?? []
  const relatedEn = en.relatedTerms ?? relatedTr
  const relatedDe = de.relatedTerms ?? relatedTr
  return {
    slug: tr.slug,
    category: tr.category,
    type: tr.type,
    difficulty: tr.difficulty,
    status: 'ACTIVE',
    adminOnly: tr.adminOnly ?? false,
    pages: ['FINANCIAL_LITERACY'],
    targetElementIds: [],
    targetInstrumentSymbols: [],
    targetTerms: tr.targetTerms ?? [tr.title],
    title: tr.title,
    shortDescription: tr.shortDescription,
    detailedDescription: tr.detailedDescription,
    howToInterpret: tr.howToInterpret ?? '',
    commonMistake: tr.commonMistake ?? '',
    example: tr.example ?? '',
    relatedTerms: relatedTr,
    learningContext: tr.learningContext ?? null,
    translations: {
      tr: {
        title: tr.title,
        shortDescription: tr.shortDescription,
        detailedDescription: tr.detailedDescription,
        howToInterpret: tr.howToInterpret ?? '',
        commonMistake: tr.commonMistake ?? '',
        example: tr.example ?? '',
        relatedTerms: relatedTr,
      },
      en: { ...en, relatedTerms: relatedEn },
      de: { ...de, relatedTerms: relatedDe },
    },
    ...extra,
  }
}

function buildCatalog() {
  const fromExisting = EXISTING_TR.map((tr) => toCard(tr, pickLocale(tr, I18N_EN, 'en'), pickLocale(tr, I18N_DE, 'de')))
  const fromNew = NEW_CARDS.map((c) => toCard(c.tr, c.en, c.de))
  const cards = [...fromExisting, ...fromNew].sort((a, b) => a.title.localeCompare(b.title, 'tr'))
  const publicCards = cards.filter((c) => !c.adminOnly)
  return {
    meta: {
      purpose: 'Finansal Okuryazarlık — genel bilgi kartı kataloğu (inceleme taslağı; DB seed değil)',
      note: 'pages yalnızca FINANCIAL_LITERACY; targetElementIds boş — portal butonuna bağlanmaz.',
      generatedAt: new Date().toISOString(),
      cardCount: cards.length,
      publicCardCount: publicCards.length,
      adminOnlyCount: cards.length - publicCards.length,
      locales: ['tr', 'en', 'de'],
      byCategory: countBy(cards, 'category'),
      byType: countBy(cards, 'type'),
      byDifficulty: countBy(cards, 'difficulty'),
    },
    cards,
  }
}

function countBy(items, key) {
  return items.reduce((acc, item) => {
    const k = item[key]
    acc[k] = (acc[k] ?? 0) + 1
    return acc
  }, {})
}

function buildHtml(catalog) {
  const rows = catalog.cards
    .map((c) => {
      const tr = c.translations.tr
      const pub = c.adminOnly ? 'admin' : 'public'
      return `<article class="card" data-slug="${c.slug}" data-category="${c.category}" data-pub="${pub}">
  <header><h2>${esc(tr.title)} <span class="slug">${esc(c.slug)}</span></h2>
  <p class="meta">${esc(c.category)} · ${esc(c.type)} · ${esc(c.difficulty)} · ${pub}</p></header>
  <section><h3>TR</h3><p><strong>Kısa:</strong> ${esc(tr.shortDescription)}</p>
  <p><strong>Detay:</strong> ${esc(tr.detailedDescription)}</p>
  <p><strong>Yorum:</strong> ${esc(tr.howToInterpret)}</p>
  ${tr.commonMistake ? `<p><strong>Hata:</strong> ${esc(tr.commonMistake)}</p>` : ''}
  ${tr.example ? `<p><strong>Örnek:</strong> ${esc(tr.example)}</p>` : ''}</section>
  <section><h3>EN</h3><p><strong>${esc(c.translations.en.title)}</strong> — ${esc(c.translations.en.shortDescription)}</p></section>
  <section><h3>DE</h3><p><strong>${esc(c.translations.de.title)}</strong> — ${esc(c.translations.de.shortDescription)}</p></section>
</article>`
    })
    .join('\n')

  return `<!DOCTYPE html>
<html lang="tr"><head><meta charset="utf-8"/><title>Finansal Okuryazarlık — Kart İnceleme</title>
<style>
body{font-family:system-ui,sans-serif;background:#0f1419;color:#e6edf3;margin:0;padding:24px}
h1{font-size:1.4rem}.stats{color:#8b949e;margin-bottom:24px}
input{width:100%;max-width:480px;padding:10px;margin-bottom:20px;border-radius:8px;border:1px solid #30363d;background:#161b22;color:#e6edf3}
.card{border:1px solid #30363d;border-radius:12px;padding:16px;margin-bottom:16px;background:#161b22}
.card h2{margin:0 0 8px;font-size:1.1rem}.slug{color:#58a6ff;font-weight:normal;font-size:.85rem}
.meta{font-size:.8rem;color:#8b949e;margin:0 0 12px}section{margin-top:12px}h3{font-size:.9rem;color:#8b949e;margin:0 0 6px}
.card[data-pub=admin]{opacity:.75;border-style:dashed}
</style></head><body>
<h1>Finansal Okuryazarlık — Genel Bilgi Kartları (${catalog.meta.cardCount})</h1>
<p class="stats">Herkese açık: ${catalog.meta.publicCardCount} · Admin-only: ${catalog.meta.adminOnlyCount} · 
${Object.entries(catalog.meta.byCategory).map(([k,v])=>`${k}: ${v}`).join(' · ')}</p>
<input type="search" id="q" placeholder="Başlık veya slug ara…" />
<div id="list">${rows}</div>
<script>
const q=document.getElementById('q');const list=document.getElementById('list');
const cards=[...list.querySelectorAll('.card')];
q.addEventListener('input',()=>{const v=q.value.toLowerCase();
cards.forEach(c=>{c.style.display=c.innerText.toLowerCase().includes(v)?'':'none';});});
</script></body></html>`
}

function esc(s) {
  return String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

mkdirSync(OUT_DIR, { recursive: true })
const catalog = buildCatalog()
writeFileSync(join(OUT_DIR, 'info-cards-literacy-catalog.json'), JSON.stringify(catalog, null, 2), 'utf8')
writeFileSync(join(OUT_DIR, 'catalog-review.html'), buildHtml(catalog), 'utf8')
console.log(`Wrote ${catalog.meta.cardCount} cards → seed-review/`)
