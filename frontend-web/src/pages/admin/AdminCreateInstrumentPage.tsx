import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  createAdminInstrument,
  type AdminExchange,
  type AdminIngestSegment,
  type AdminInstrumentType,
} from '../../features/admin/api/adminInstrumentsApi'
import { PortalAlert } from '../../shared/components/PortalAlert'

const TYPE_OPTIONS: AdminInstrumentType[] = ['CRYPTO', 'STOCK', 'FX', 'FUND', 'BOND', 'EUROBOND', 'DEPOSIT']
const SEGMENT_OPTIONS: AdminIngestSegment[] = ['CRYPTO', 'BIST', 'NASDAQ']
const EXCHANGES_BY_TYPE: Record<AdminInstrumentType, AdminExchange[]> = {
  CRYPTO: ['BINANCE'],
  STOCK: ['BIST', 'NASDAQ', 'FINNHUB', 'YAHOO'],
  FX: ['TCMB'],
  FUND: ['TEFAS'],
  BOND: ['TCMB'],
  EUROBOND: ['YAHOO'],
  DEPOSIT: ['TCMB'],
}

type FormState = {
  symbol: string
  name: string
  type: AdminInstrumentType
  exchange: AdminExchange
  segment: AdminIngestSegment | ''
}

const DEFAULT_FORM: FormState = {
  symbol: '',
  name: '',
  type: 'STOCK',
  exchange: 'BIST',
  segment: 'BIST',
}

export function AdminCreateInstrumentPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<FormState>(DEFAULT_FORM)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const allowedExchanges = EXCHANGES_BY_TYPE[form.type]
  const allowedSegments =
    form.type === 'CRYPTO'
      ? (['CRYPTO'] as AdminIngestSegment[])
      : form.type === 'STOCK'
        ? (SEGMENT_OPTIONS.filter((segment) => segment !== 'CRYPTO') as AdminIngestSegment[])
        : ([] as AdminIngestSegment[])

  const defaultSegmentFor = (type: AdminInstrumentType, exchange: AdminExchange): AdminIngestSegment | '' => {
    if (type === 'CRYPTO') {
      return 'CRYPTO'
    }
    if (type === 'STOCK') {
      return exchange === 'BIST' ? 'BIST' : 'NASDAQ'
    }
    return ''
  }

  const onSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setSuccess(null)
    const symbol = form.symbol.trim().toUpperCase()
    const name = form.name.trim()
    if (!symbol || !name) {
      setError('Symbol ve isim zorunludur.')
      return
    }
    if (form.segment && !allowedSegments.includes(form.segment)) {
      setError('Secilen type icin segment uyumsuz.')
      return
    }
    if (!allowedExchanges.includes(form.exchange)) {
      setError('Secilen type icin exchange uyumsuz.')
      return
    }
    setBusy(true)
    try {
      const instrumentId = await createAdminInstrument({
        symbol,
        name,
        type: form.type,
        exchange: form.exchange,
        segment: form.segment || undefined,
      })
      setSuccess(`Enstrüman oluşturuldu (ID: ${instrumentId}). Ingest registry sayfasına yönlendiriliyorsun...`)
      window.setTimeout(() => {
        navigate('/admin/kpi/ingest-registry')
      }, 700)
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Enstrüman oluşturulamadı.'
      setError(message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="fi-admin-page">
      <header className="fi-admin-page-head">
        <div>
          <h1 className="fi-admin-h1">Yeni varlık ekle</h1>
          <p className="fi-admin-lead">
            Admin panelinden yeni enstrüman ekleyin ve ingest segmentini seçerek otomatik ingest kaydı oluşturun.
          </p>
        </div>
      </header>
      {error ? <PortalAlert variant="error">{error}</PortalAlert> : null}
      {success ? <PortalAlert variant="success">{success}</PortalAlert> : null}
      <section className="fi-admin-card">
        <form className="fi-admin-form-grid" onSubmit={onSubmit}>
          <label className="fi-admin-form-field">
            <span>Symbol</span>
            <input
              value={form.symbol}
              onChange={(e) => setForm((prev) => ({ ...prev, symbol: e.target.value }))}
              placeholder="BTCUSDT"
              autoComplete="off"
            />
          </label>
          <label className="fi-admin-form-field">
            <span>Name</span>
            <input
              value={form.name}
              onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
              placeholder="Bitcoin / USDT"
              autoComplete="off"
            />
          </label>
          <label className="fi-admin-form-field">
            <span>Type</span>
            <select
              value={form.type}
              onChange={(e) => {
                const nextType = e.target.value as AdminInstrumentType
                const nextExchanges = EXCHANGES_BY_TYPE[nextType]
                const nextExchange = nextExchanges[0]
                setForm((prev) => ({
                  ...prev,
                  type: nextType,
                  exchange: nextExchange,
                  segment: defaultSegmentFor(nextType, nextExchange),
                }))
              }}
            >
              {TYPE_OPTIONS.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </label>
          <label className="fi-admin-form-field">
            <span>Exchange</span>
            <select
              value={form.exchange}
              onChange={(e) => {
                const nextExchange = e.target.value as AdminExchange
                setForm((prev) => ({
                  ...prev,
                  exchange: nextExchange,
                  segment:
                    prev.segment && allowedSegments.includes(prev.segment)
                      ? prev.segment
                      : defaultSegmentFor(prev.type, nextExchange),
                }))
              }}
            >
              {allowedExchanges.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </label>
          <label className="fi-admin-form-field">
            <span>Ingest segment (optional)</span>
            <select
              value={form.segment}
              onChange={(e) => setForm((prev) => ({ ...prev, segment: e.target.value as AdminIngestSegment | '' }))}
            >
              <option value="">Seçme</option>
              {allowedSegments.map((opt) => (
                <option key={opt} value={opt}>
                  {opt}
                </option>
              ))}
            </select>
          </label>
          <div className="fi-admin-form-actions">
            <button type="submit" className="fi-admin-dir-btn fi-admin-dir-btn--primary" disabled={busy}>
              {busy ? 'Kaydediliyor...' : 'Varlık ekle'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
