import type { ReactNode } from 'react'
import type { StatCardCopy, StatIconId } from '../faizVadeliDashboardCopy'
import { HelpTerm } from '../../../components/help/HelpTerm'
import type { PortalPageKey } from '../../../types/infoCards'

function IconBank({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden>
      <path
        fill="currentColor"
        d="M3 21h18v-2H3v2zm0-4h18v-1H3v1zM12 2 3 7v2h18V7L12 2zm0 2.2 6.3 3.3H5.7L12 4.2zM5 11h3v6H5v-6zm5 0h4v6h-4v-6zm6 0h3v6h-3v-6z"
      />
    </svg>
  )
}

function IconCoins({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden>
      <path
        fill="currentColor"
        d="M12 2C8 2 5 3.8 5 6c0 1.6 1.4 2.9 3.5 3.5C6.1 10.4 5 11.6 5 13v1c0 2.2 3 4 7 4s7-1.8 7-4v-1c0-1.4-1.1-2.6-3.5-3.5C16.6 8.9 18 7.6 18 6c0-2.2-3-4-6-4zm0 2c2.8 0 5 .9 5 2s-2.2 2-5 2-5-.9-5-2 2.2-2 5-2zm-5 9.9V13c0-.5.8-1.2 2.3-1.7.5.2 1.7.4 2.7.5v3.3c-2.5-.2-5-1-5-2.2zm7 1.6v-3.3c1-.1 2.2-.3 2.7-.5 1.5.5 2.3 1.2 2.3 1.7v1.9c0 1.2-2.5 2-5 2.2z"
      />
    </svg>
  )
}

function IconDocChart({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden>
      <path
        fill="currentColor"
        d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm0 3.5L18.5 10H14V5.5zM8 18v-2h8v2H8zm0-4v-2h5v2H8z"
        opacity=".9"
      />
      <path fill="none" stroke="currentColor" strokeWidth="1.4" d="M8 13h2l2-2 2 2h3" />
    </svg>
  )
}

function IconSpread({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden>
      <path fill="currentColor" d="M4 19h4v-6H4v6zm6 0h4V5h-4v14zm6 0h4v-9h-4v9z" />
    </svg>
  )
}

function IconBell({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden>
      <path
        fill="currentColor"
        d="M12 22a2 2 0 0 0 2-2h-4a2 2 0 0 0 2 2zm6-6V11c0-3.1-1.6-5.6-4.5-6.3V4a1.5 1.5 0 0 0-3 0v.7C7.6 5.4 6 7.9 6 11v5l-2 2v1h16v-1l-2-2z"
      />
    </svg>
  )
}

function StatIcon({ id }: { id: StatIconId }) {
  const cls = 'fi-faiz-stat-icon-svg'
  switch (id) {
    case 'bank':
      return <IconBank className={cls} />
    case 'coins':
      return <IconCoins className={cls} />
    case 'doc':
      return <IconDocChart className={cls} />
    case 'spread':
      return <IconSpread className={cls} />
    case 'bell':
      return <IconBell className={cls} />
    default:
      return null
  }
}

export function FaizVadeliStatCard({
  stat,
  valueSkeleton,
  valueSkeletonAria,
  interactive,
  onActivate,
  interactiveAriaLabel,
  interactiveTitle,
  headEndSlot,
  helpTerm,
  helpPageKey,
  helpElementId,
}: {
  stat: StatCardCopy
  valueSkeleton?: boolean
  valueSkeletonAria?: string
  interactive?: boolean
  onActivate?: () => void
  interactiveAriaLabel?: string
  interactiveTitle?: string
  /** Extra control(s) beside the title (e.g. maturity select). Uses article+role=button instead of native button. */
  headEndSlot?: ReactNode
  helpTerm?: string
  helpPageKey?: PortalPageKey
  helpElementId?: string
}) {
  const titleNode =
    helpTerm && helpPageKey ? (
      <HelpTerm term={helpTerm} pageKey={helpPageKey} elementId={helpElementId}>
        {stat.title}
      </HelpTerm>
    ) : (
      stat.title
    )
  const deltaClass =
    stat.deltaTone === 'negative'
      ? 'fi-faiz-delta fi-faiz-delta--neg'
      : stat.deltaTone === 'positive'
        ? 'fi-faiz-delta fi-faiz-delta--pos'
        : stat.deltaTone === 'neutral'
          ? 'fi-faiz-delta fi-faiz-delta--muted'
          : 'fi-faiz-delta'

  const canActivate = Boolean(interactive && onActivate)
  const useArticleShell = Boolean(headEndSlot && canActivate)
  const cardClass =
    'fi-faiz-stat-card' +
    (canActivate ? ' fi-faiz-stat-card--clickable' : '') +
    (headEndSlot ? ' fi-faiz-stat-card--has-head-slot' : '')

  const head = (
    <div className="fi-faiz-stat-card-head">
      <span className={`fi-faiz-stat-icon fi-faiz-stat-icon--${stat.icon}`} aria-hidden>
        <StatIcon id={stat.icon} />
      </span>
      {headEndSlot ? (
        <div className="fi-faiz-stat-head-text">
          <div className="fi-faiz-stat-title-row">
            <h3 className="fi-faiz-stat-title">{titleNode}</h3>
            {headEndSlot}
          </div>
        </div>
      ) : (
        <h3 className="fi-faiz-stat-title">{titleNode}</h3>
      )}
    </div>
  )

  const inner = (
    <>
      {head}
      {valueSkeleton ? (
        <div
          className="markets-skeleton-row fi-faiz-stat-value-skel"
          aria-busy="true"
          aria-label={valueSkeletonAria}
        />
      ) : (
        <p className="fi-faiz-stat-value">{stat.value}</p>
      )}
      <div className="fi-faiz-stat-meta">
        {stat.sub1 || stat.badge ? (
          <div className="fi-faiz-stat-meta-row">
            {stat.sub1 ? <span className="fi-faiz-stat-sub">{stat.sub1}</span> : null}
            {stat.badge ? <span className="fi-faiz-stat-badge">{stat.badge}</span> : null}
          </div>
        ) : null}
        {stat.sub2 ? <span className="fi-faiz-stat-sub">{stat.sub2}</span> : null}
        {stat.delta ? <span className={deltaClass}>{stat.delta}</span> : null}
      </div>
    </>
  )

  if (useArticleShell) {
    return (
      <article
        className={cardClass}
        role="button"
        tabIndex={0}
        onClick={(e) => {
          if ((e.target as HTMLElement).closest('[data-fi-faiz-skip-card-activate]')) {
            return
          }
          onActivate?.()
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault()
            onActivate?.()
          }
        }}
        aria-label={interactiveAriaLabel ?? stat.title}
        title={interactiveTitle}
      >
        {inner}
      </article>
    )
  }

  if (canActivate && onActivate) {
    return (
      <button
        type="button"
        className={cardClass}
        onClick={onActivate}
        aria-label={interactiveAriaLabel ?? stat.title}
        title={interactiveTitle}
      >
        {inner}
      </button>
    )
  }

  return (
    <article className={cardClass}>
      {inner}
    </article>
  )
}
