import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import {
  createAdminInstrument,
  INGEST_SEGMENT_PRESETS,
  type IngestMarketSegment,
} from '../../features/admin/api/adminInstrumentsApi'
import { PortalAlert } from '../../shared/components/PortalAlert'

type FormState = {
  symbol: string
  name: string
  marketSegment: IngestMarketSegment
}

const DEFAULT_FORM: FormState = {
  symbol: '',
  name: '',
  marketSegment: 'BIST',
}

const MARKET_SEGMENTS: IngestMarketSegment[] = ['CRYPTO', 'BIST', 'NASDAQ']

export function AdminCreateInstrumentPage() {
  const { t } = useTranslation('admin')
  const navigate = useNavigate()
  const [form, setForm] = useState<FormState>(DEFAULT_FORM)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const onSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setSuccess(null)
    const symbol = form.symbol.trim().toUpperCase()
    const name = form.name.trim()
    if (!symbol || !name) {
      setError(t('createInstrumentPage.errorRequired'))
      return
    }
    const preset = INGEST_SEGMENT_PRESETS[form.marketSegment]
    setBusy(true)
    try {
      const instrumentId = await createAdminInstrument({
        symbol,
        name,
        type: preset.type,
        exchange: preset.exchange,
        segment: preset.segment,
      })
      setSuccess(t('createInstrumentPage.success', { id: instrumentId }))
      window.setTimeout(() => {
        navigate('/admin/kpi/ingest-registry')
      }, 700)
    } catch (e) {
      const message = e instanceof Error ? e.message : t('createInstrumentPage.errorGeneric')
      setError(message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="fi-admin-page">
      <header className="fi-admin-page-head">
        <div>
          <h1 className="fi-admin-h1">{t('createInstrumentPage.title')}</h1>
          <p className="fi-admin-lead">{t('createInstrumentPage.lead')}</p>
        </div>
      </header>
      {error ? <PortalAlert variant="error">{error}</PortalAlert> : null}
      {success ? <PortalAlert variant="success">{success}</PortalAlert> : null}
      <section className="fi-admin-card">
        <form className="fi-admin-form-grid" onSubmit={onSubmit}>
          <label className="fi-admin-form-field">
            <span>{t('createInstrumentPage.marketSegment')}</span>
            <select
              value={form.marketSegment}
              onChange={(e) =>
                setForm((prev) => ({ ...prev, marketSegment: e.target.value as IngestMarketSegment }))
              }
            >
              {MARKET_SEGMENTS.map((segment) => (
                <option key={segment} value={segment}>
                  {t(`createInstrumentPage.segment.${segment}`)}
                </option>
              ))}
            </select>
          </label>
          <label className="fi-admin-form-field">
            <span>{t('createInstrumentPage.symbol')}</span>
            <input
              value={form.symbol}
              onChange={(e) => setForm((prev) => ({ ...prev, symbol: e.target.value }))}
              placeholder={t('createInstrumentPage.symbolPlaceholder')}
              autoComplete="off"
            />
          </label>
          <label className="fi-admin-form-field">
            <span>{t('createInstrumentPage.name')}</span>
            <input
              value={form.name}
              onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
              placeholder={t('createInstrumentPage.namePlaceholder')}
              autoComplete="off"
            />
          </label>
          <div className="fi-admin-form-actions">
            <button type="submit" className="fi-admin-dir-btn fi-admin-dir-btn--primary" disabled={busy}>
              {busy ? t('createInstrumentPage.submitting') : t('createInstrumentPage.submit')}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
