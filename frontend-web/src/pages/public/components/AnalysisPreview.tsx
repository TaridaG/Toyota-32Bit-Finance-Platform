import { memo } from 'react'

const RANGE_BUTTONS = ['1h', '6h', '24h', '7d', '30d', '90d', '1y', '5y'] as const

const CANDLES = [
  { x: 78, open: 186, close: 194, high: 180, low: 204 },
  { x: 94, open: 194, close: 204, high: 188, low: 212 },
  { x: 110, open: 204, close: 198, high: 192, low: 214 },
  { x: 126, open: 198, close: 188, high: 182, low: 206 },
  { x: 142, open: 184, close: 168, high: 160, low: 194 },
  { x: 158, open: 166, close: 176, high: 158, low: 182 },
  { x: 174, open: 178, close: 188, high: 170, low: 194 },
  { x: 190, open: 190, close: 172, high: 166, low: 198 },
  { x: 206, open: 174, close: 154, high: 146, low: 184 },
  { x: 222, open: 156, close: 132, high: 124, low: 166 },
  { x: 238, open: 134, close: 136, high: 126, low: 142 },
  { x: 254, open: 138, close: 128, high: 122, low: 146 },
  { x: 270, open: 130, close: 112, high: 104, low: 138 },
  { x: 286, open: 114, close: 96, high: 90, low: 122 },
  { x: 302, open: 100, close: 82, high: 74, low: 110 },
  { x: 318, open: 86, close: 96, high: 80, low: 102 },
  { x: 334, open: 98, close: 108, high: 90, low: 116 },
  { x: 350, open: 110, close: 118, high: 102, low: 124 },
  { x: 366, open: 120, close: 126, high: 112, low: 132 },
  { x: 382, open: 128, close: 140, high: 120, low: 146 },
  { x: 398, open: 142, close: 134, high: 128, low: 150 },
  { x: 414, open: 136, close: 146, high: 130, low: 154 },
  { x: 430, open: 148, close: 138, high: 132, low: 156 },
  { x: 446, open: 140, close: 150, high: 134, low: 158 },
  { x: 462, open: 152, close: 162, high: 146, low: 168 },
  { x: 478, open: 164, close: 158, high: 152, low: 172 },
  { x: 494, open: 160, close: 154, high: 148, low: 168 },
  { x: 510, open: 156, close: 150, high: 144, low: 164 },
  { x: 526, open: 152, close: 144, high: 138, low: 160 },
  { x: 542, open: 146, close: 142, high: 136, low: 154 },
  { x: 558, open: 144, close: 138, high: 132, low: 152 },
  { x: 574, open: 140, close: 146, high: 134, low: 154 },
  { x: 590, open: 148, close: 144, high: 138, low: 156 },
  { x: 606, open: 146, close: 140, high: 134, low: 152 },
  { x: 622, open: 142, close: 136, high: 130, low: 148 },
  { x: 638, open: 138, close: 132, high: 126, low: 144 },
] as const

const VOLUMES = [28, 34, 30, 26, 38, 44, 40, 36, 52, 60, 42, 48, 58, 64, 46, 38, 34, 42, 32, 36, 60, 44, 52, 40, 34, 30, 56, 38, 42, 34, 30, 28, 36, 32, 30, 40] as const

const PRICE_LABELS = [
  { x: 686, y: 44, label: '66.00' },
  { x: 686, y: 74, label: '65.50' },
  { x: 686, y: 104, label: '65.00' },
  { x: 686, y: 134, label: '64.50' },
  { x: 686, y: 164, label: '64.00' },
  { x: 686, y: 194, label: '63.50' },
  { x: 686, y: 224, label: '63.00' },
  { x: 686, y: 254, label: '62.50' },
  { x: 686, y: 284, label: '62.00' },
  { x: 686, y: 314, label: '61.50' },
] as const

const TIME_LABELS = [
  { x: 44, label: '23 May' },
  { x: 128, label: '12:00' },
  { x: 238, label: '24 May' },
  { x: 366, label: '22:00' },
  { x: 468, label: '25 May 15:30', active: true },
  { x: 590, label: '26 May' },
  { x: 664, label: '12:00' },
  { x: 748, label: '27 May' },
] as const

const SIDE_TOOLS = [
  { id: 'crosshair', icon: <IconCrosshair /> },
  { id: 'trend', icon: <IconTrend /> },
  { id: 'measure', icon: <IconMeasure /> },
  { id: 'fib', icon: <IconFib /> },
  { id: 'brush', icon: <IconBrush /> },
  { id: 'text', icon: <IconText /> },
  { id: 'path', icon: <IconPath /> },
  { id: 'eye', icon: <IconEye /> },
  { id: 'trash', icon: <IconTrash /> },
] as const

const RSI_PATH =
  'M6 54 C32 68, 48 72, 70 58 S118 38, 148 30 S204 26, 238 20 S286 12, 324 16 S370 28, 406 18 S448 8, 486 14 S534 38, 574 50 S622 58, 658 60 S704 46, 742 36'

export const AnalysisPreview = memo(function AnalysisPreview() {
  return (
    <div className="analysis-preview" aria-hidden="true">
      <div className="analysis-preview-shell">
        <div className="analysis-preview-topbar">
          <div className="analysis-preview-symbol">
            <span className="analysis-preview-symbol-badge">
              <IconBank />
            </span>
            <div className="analysis-preview-symbol-copy">
              <strong>AKBNK</strong>
              <span>Akbank</span>
            </div>
            <button type="button" className="analysis-preview-star-btn">
              ☆
            </button>
          </div>

          <div className="analysis-preview-topbar-right">
            <div className="analysis-preview-range-group">
              {RANGE_BUTTONS.map((range) => (
                <button
                  key={range}
                  type="button"
                  className={`analysis-preview-range-btn${range === '24h' ? ' is-active' : ''}`}
                >
                  {range}
                </button>
              ))}
            </div>

            <div className="analysis-preview-top-actions">
              <button type="button" className="analysis-preview-icon-btn">
                <IconTune />
              </button>
              <button type="button" className="analysis-preview-draw-btn">
                <IconSpark />
                <span>Cizim</span>
              </button>
              <button type="button" className="analysis-preview-icon-btn">
                <IconExpand />
              </button>
            </div>
          </div>
        </div>

        <div className="analysis-preview-subbar">
          <div className="analysis-preview-subtools">
            <button type="button" className="analysis-preview-subtool">
              <IconWave />
              <span>Gostergeler</span>
            </button>
            <button type="button" className="analysis-preview-subtool">
              <IconCompare />
              <span>Kiyasla</span>
            </button>
            <button type="button" className="analysis-preview-subtool">
              <IconBell />
              <span>Alarm</span>
            </button>
          </div>

          <div className="analysis-preview-history-actions">
            <button type="button" className="analysis-preview-ghost-btn">
              <IconUndo />
            </button>
            <button type="button" className="analysis-preview-ghost-btn">
              <IconRedo />
            </button>
          </div>
        </div>

        <div className="analysis-preview-workbench">
          <div className="analysis-preview-side-tools">
            {SIDE_TOOLS.map((tool, index) => (
              <button
                key={tool.id}
                type="button"
                className={`analysis-preview-side-btn${index === 0 ? ' is-active' : ''}`}
              >
                {tool.icon}
              </button>
            ))}
          </div>

          <div className="analysis-preview-chart-wrap">
            <div className="analysis-preview-main-chart">
              <div className="analysis-preview-tooltip">
                <div className="analysis-preview-tooltip-date">25 May 15:30</div>
                <dl className="analysis-preview-tooltip-list">
                  <div>
                    <dt>Acilis</dt>
                    <dd>63.40</dd>
                  </div>
                  <div>
                    <dt>Yuksek</dt>
                    <dd>63.70</dd>
                  </div>
                  <div>
                    <dt>Dusuk</dt>
                    <dd>63.20</dd>
                  </div>
                  <div className="is-strong">
                    <dt>Kapanis</dt>
                    <dd>63.60</dd>
                  </div>
                  <div className="is-ma20">
                    <dt>MA 20</dt>
                    <dd>63.15</dd>
                  </div>
                  <div className="is-ma50">
                    <dt>MA 50</dt>
                    <dd>62.80</dd>
                  </div>
                </dl>
              </div>

              <svg className="analysis-preview-main-svg" viewBox="0 0 760 320" role="presentation">
                <defs>
                  <linearGradient id="analysisPreviewVolumeFill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="0%" stopColor="rgba(59, 130, 246, 0.5)" />
                    <stop offset="100%" stopColor="rgba(59, 130, 246, 0.12)" />
                  </linearGradient>
                </defs>

                <g className="analysis-preview-grid">
                  <line x1="20" y1="30" x2="650" y2="30" />
                  <line x1="20" y1="72" x2="650" y2="72" />
                  <line x1="20" y1="114" x2="650" y2="114" />
                  <line x1="20" y1="156" x2="650" y2="156" />
                  <line x1="20" y1="198" x2="650" y2="198" />
                  <line x1="20" y1="240" x2="650" y2="240" />
                  <line x1="20" y1="282" x2="650" y2="282" />
                  <line x1="52" y1="10" x2="52" y2="300" />
                  <line x1="148" y1="10" x2="148" y2="300" />
                  <line x1="244" y1="10" x2="244" y2="300" />
                  <line x1="340" y1="10" x2="340" y2="300" />
                  <line x1="436" y1="10" x2="436" y2="300" />
                  <line x1="532" y1="10" x2="532" y2="300" />
                  <line x1="628" y1="10" x2="628" y2="300" />
                </g>

                <polyline
                  className="analysis-preview-ma20"
                  points="20,210 68,206 116,196 164,178 212,154 260,126 308,104 356,96 404,112 452,136 500,164 548,184 596,198 644,188"
                />
                <polyline
                  className="analysis-preview-ma50"
                  points="20,232 68,228 116,220 164,208 212,194 260,180 308,166 356,154 404,146 452,148 500,154 548,162 596,172 644,180"
                />
                <line className="analysis-preview-current-line" x1="20" y1="168" x2="650" y2="168" />

                {CANDLES.map((candle) => {
                  const rising = candle.close < candle.open
                  const bodyTop = Math.min(candle.open, candle.close)
                  const bodyHeight = Math.max(Math.abs(candle.close - candle.open), 6)
                  return (
                    <g key={candle.x} className={`analysis-preview-candle ${rising ? 'is-up' : 'is-down'}`}>
                      <line x1={candle.x} y1={candle.high} x2={candle.x} y2={candle.low} />
                      <rect x={candle.x - 5.5} y={bodyTop} width="11" height={bodyHeight} rx="2.6" />
                    </g>
                  )
                })}

                {VOLUMES.map((height, index) => (
                  <rect
                    key={`${height}-${index}`}
                    className={`analysis-preview-volume-bar${index % 3 === 1 ? ' is-alt' : ''}`}
                    x={72 + index * 16}
                    y={286 - height}
                    width="10"
                    height={height}
                    rx="2"
                  />
                ))}

                {PRICE_LABELS.map((item) => (
                  <text key={item.label} className="analysis-preview-price-scale" x={item.x} y={item.y}>
                    {item.label}
                  </text>
                ))}

                <text className="analysis-preview-volume-scale" x="700" y="272">
                  5M
                </text>
                <text className="analysis-preview-volume-scale" x="694" y="300">
                  2.5M
                </text>
              </svg>

              <div className="analysis-preview-price-tag">63.60</div>
            </div>

            <div className="analysis-preview-rsi-panel">
              <div className="analysis-preview-rsi-title">
                <span>RSI (14)</span>
                <strong>54.21</strong>
              </div>

              <svg className="analysis-preview-rsi-svg" viewBox="0 0 760 110" role="presentation">
                <g className="analysis-preview-rsi-grid">
                  <line x1="20" y1="26" x2="650" y2="26" />
                  <line x1="20" y1="82" x2="650" y2="82" />
                </g>
                <path className="analysis-preview-rsi-fill" d={`${RSI_PATH} L742 94 L6 94 Z`} />
                <path className="analysis-preview-rsi-line" d={RSI_PATH} />
                <line className="analysis-preview-rsi-level" x1="20" y1="26" x2="650" y2="26" />
                <line className="analysis-preview-rsi-level" x1="20" y1="82" x2="650" y2="82" />
                <circle className="analysis-preview-rsi-dot-glow" cx="698" cy="54" r="14" />
                <circle className="analysis-preview-rsi-dot" cx="698" cy="54" r="5" />

                <text className="analysis-preview-rsi-scale" x="684" y="30">
                  70.00
                </text>
                <text className="analysis-preview-rsi-scale" x="684" y="86">
                  30.00
                </text>

                {TIME_LABELS.map((item) => (
                  <g key={item.label} transform={`translate(${item.x}, 102)`}>
                    {item.active ? (
                      <rect className="analysis-preview-time-pill" x="-30" y="-16" width="60" height="18" rx="6" />
                    ) : null}
                    <text className={`analysis-preview-time-scale${item.active ? ' is-active' : ''}`} textAnchor="middle">
                      {item.label}
                    </text>
                  </g>
                ))}
              </svg>

              <div className="analysis-preview-rsi-tag">54.21</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
})

function IconBank() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <circle cx="8" cy="8" r="6.5" fill="none" stroke="currentColor" strokeWidth="1.4" />
      <path d="M4.5 6.1h7M5.2 9.9h5.6" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
    </svg>
  )
}

function IconTune() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M4 3v10M12 3v10M4 6.2h4M8 9.8h4" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
    </svg>
  )
}

function IconSpark() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M2.5 11.5 6.4 7.6l2.2 2.2L13.5 5" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M10.7 5h2.8v2.8" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function IconExpand() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M6 2.7H2.7V6M10 2.7h3.3V6M6 13.3H2.7V10M10 13.3h3.3V10" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function IconWave() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M1.8 9.2c1.4 0 1.8-4 3.2-4s1.8 5.6 3.2 5.6S10 4.8 11.4 4.8s1.8 4.4 2.8 4.4" fill="none" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
    </svg>
  )
}

function IconCompare() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <circle cx="6.2" cy="8" r="4.2" fill="none" stroke="currentColor" strokeWidth="1.2" />
      <path d="M11 11.3 14 14.2M10.8 8h3.5M12.55 6.25v3.5" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
    </svg>
  )
}

function IconBell() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M8 3.1a2.4 2.4 0 0 1 2.4 2.4v1.2c0 .9.3 1.8.9 2.5l.8 1H4l.8-1a4 4 0 0 0 .9-2.5V5.5A2.4 2.4 0 0 1 8 3.1Z" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinejoin="round" />
      <path d="M6.4 11.3a1.7 1.7 0 0 0 3.2 0" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
    </svg>
  )
}

function IconUndo() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M6.4 4 3 7.2l3.4 3.1M3.6 7.2H10a3 3 0 1 1 0 6" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function IconRedo() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="m9.6 4 3.4 3.2-3.4 3.1M12.4 7.2H6a3 3 0 1 0 0 6" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function IconCrosshair() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M8 2.5v11M2.5 8h11" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
      <circle cx="8" cy="8" r="1.7" fill="none" stroke="currentColor" strokeWidth="1.3" />
    </svg>
  )
}

function IconTrend() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M2.7 11.8 6.1 8.6l2.3 1.9 4.9-5.2" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function IconMeasure() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M3.2 12.2h9.6M4.6 4.2h6.8M4.6 4.2v8M11.4 4.2v8" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  )
}

function IconFib() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M3 12.5h10M3.8 10h8.4M4.6 7.5h6.8M5.4 5h5.2M6.2 2.5h3.6" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
    </svg>
  )
}

function IconBrush() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M10.9 2.7 13.3 5l-6 6-3.1.8.8-3.1 5.9-6Z" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinejoin="round" />
    </svg>
  )
}

function IconText() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M3 3.5h10M8 3.5v9" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  )
}

function IconPath() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <circle cx="3.1" cy="11.7" r="1.3" fill="none" stroke="currentColor" strokeWidth="1.1" />
      <circle cx="8" cy="4.4" r="1.3" fill="none" stroke="currentColor" strokeWidth="1.1" />
      <circle cx="12.8" cy="9.8" r="1.3" fill="none" stroke="currentColor" strokeWidth="1.1" />
      <path d="M4.1 10.8 7 5.4m2 0 2.8 3.3" fill="none" stroke="currentColor" strokeWidth="1.1" strokeLinecap="round" />
    </svg>
  )
}

function IconEye() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M1.8 8s2.1-3.3 6.2-3.3S14.2 8 14.2 8s-2.1 3.3-6.2 3.3S1.8 8 1.8 8Z" fill="none" stroke="currentColor" strokeWidth="1.2" />
      <circle cx="8" cy="8" r="1.8" fill="none" stroke="currentColor" strokeWidth="1.2" />
    </svg>
  )
}

function IconTrash() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M4.2 4.5h7.6m-6.8 0V3.2h6v1.3m-5.5 1.2v6m2.5-6v6m2.5-6v6M4.8 13h6.4" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
    </svg>
  )
}
