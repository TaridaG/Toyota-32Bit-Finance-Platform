import type { ReactNode } from 'react'

export type SimulatorMetric = {
  label: string
  value: string
}

export function FaizVadeliSimulatorCard({
  title,
  children,
  footer,
}: {
  title: string
  children: ReactNode
  footer?: ReactNode
}) {
  return (
    <aside className="fi-faiz-simulator-card">
      <div className="fi-faiz-simulator-card-inner">
        <h4 className="fi-faiz-simulator-card-title">{title}</h4>
        {children}
        {footer}
      </div>
    </aside>
  )
}

export function SimulatorMetrics({ rows }: { rows: SimulatorMetric[] }) {
  return (
    <dl className="fi-faiz-simulator-metrics">
      {rows.map((row) => (
        <div key={row.label}>
          <dt>{row.label}</dt>
          <dd>{row.value}</dd>
        </div>
      ))}
    </dl>
  )
}

export function SimulatorDivider() {
  return <div className="fi-faiz-simulator-divider" role="separator" />
}

export function SimulatorSectionTitle({ children }: { children: ReactNode }) {
  return <h5 className="fi-faiz-simulator-section-title">{children}</h5>
}

export function SimulatorHint({ children }: { children: ReactNode }) {
  return <p className="fi-faiz-simulator-hint">{children}</p>
}

export function SimulatorDisclaimer({ children }: { children: ReactNode }) {
  return <p className="fi-faiz-simulator-disclaimer">{children}</p>
}

export function SimulatorLabelField({
  label,
  children,
  className,
}: {
  label: string
  children: ReactNode
  className?: string
}) {
  return (
    <label className={`fi-faiz-simulator-field${className ? ` ${className}` : ''}`}>
      <span className="fi-faiz-simulator-field-label">{label}</span>
      {children}
    </label>
  )
}

export function SimulatorInput({
  className,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input className={`fi-faiz-simulator-input${className ? ` ${className}` : ''}`} {...props} />
}

export function SimulatorFieldRow({ children }: { children: ReactNode }) {
  return <div className="fi-faiz-simulator-field-row">{children}</div>
}

export function SimulatorPickButton({
  title,
  ariaLabel,
  onClick,
}: {
  title: string
  ariaLabel: string
  onClick: () => void
}) {
  return (
    <button type="button" className="fi-faiz-simulator-pick-btn" title={title} aria-label={ariaLabel} onClick={onClick}>
      ◎
    </button>
  )
}
