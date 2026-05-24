import type { ReactNode } from 'react'

export type PortalAlertVariant = 'error' | 'warning' | 'success' | 'info'

type Props = {
  variant: PortalAlertVariant
  title?: string
  children: ReactNode
  className?: string
}

const ICON: Record<PortalAlertVariant, string> = {
  error: '!',
  warning: '!',
  success: '✓',
  info: 'i',
}

export function PortalAlert({ variant, title, children, className }: Props) {
  const classes = ['portal-alert', `portal-alert--${variant}`, className].filter(Boolean).join(' ')
  return (
    <div className={classes} role="alert">
      <span className="portal-alert__icon" aria-hidden="true">
        {ICON[variant]}
      </span>
      <div className="portal-alert__body">
        {title ? <p className="portal-alert__title">{title}</p> : null}
        <div className="portal-alert__message">{children}</div>
      </div>
    </div>
  )
}
