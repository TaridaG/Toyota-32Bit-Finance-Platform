import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { AdminUserListItem } from '../../features/admin/api/adminUserDirectoryApi'
import {
  deleteAdminUser,
  freezeAdminUser,
  sendAdminUserMessage,
  unfreezeAdminUser,
} from '../../features/admin/api/adminUserActionsApi'
import { PortalAlert } from '../../shared/components/PortalAlert'

type ModalKind = 'message' | 'freeze' | 'unfreeze' | 'delete' | null

type Props = {
  user: AdminUserListItem | null
  kind: ModalKind
  onClose: () => void
  onSuccess: () => void
}

export function AdminUserDirectoryModals({ user, kind, onClose, onSuccess }: Props) {
  const { t } = useTranslation('admin')
  const [text, setText] = useState('')
  const [blockEmail, setBlockEmail] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (!user || !kind) {
    return null
  }

  const handleSubmit = async () => {
    setError(null)
    setBusy(true)
    try {
      if (kind === 'message') {
        await sendAdminUserMessage(user.id, text)
      } else if (kind === 'freeze') {
        await freezeAdminUser(user.id, text.trim() || undefined)
      } else if (kind === 'unfreeze') {
        await unfreezeAdminUser(user.id)
      } else if (kind === 'delete') {
        await deleteAdminUser(user.id, blockEmail)
      }
      setText('')
      setBlockEmail(false)
      onSuccess()
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setBusy(false)
    }
  }

  const title =
    kind === 'message'
      ? t('totalUsersPage.directory.modalMessageTitle', { user: user.username })
      : kind === 'freeze'
        ? t('totalUsersPage.directory.modalFreezeTitle', { user: user.username })
        : kind === 'delete'
          ? t('totalUsersPage.directory.modalDeleteTitle')
          : t('totalUsersPage.directory.modalUnfreezeTitle', { user: user.username })

  return (
    <div className="fi-admin-modal-backdrop" role="presentation" onClick={onClose}>
      <div
        className="fi-admin-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="fi-admin-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 id="fi-admin-modal-title" className="fi-admin-modal-title">
          {title}
        </h3>
        {kind === 'message' ? (
          <>
            <p className="fi-admin-modal-lead">{t('totalUsersPage.directory.modalMessageLead')}</p>
            <label className="fi-admin-dir-field">
              <span>{t('totalUsersPage.directory.modalMessageLabel')}</span>
              <textarea
                className="fi-admin-modal-textarea"
                rows={5}
                value={text}
                onChange={(e) => setText(e.target.value)}
                maxLength={2000}
              />
            </label>
          </>
        ) : null}
        {kind === 'freeze' ? (
          <>
            <p className="fi-admin-modal-lead">{t('totalUsersPage.directory.modalFreezeLead')}</p>
            <label className="fi-admin-dir-field">
              <span>{t('totalUsersPage.directory.modalFreezeReason')}</span>
              <textarea
                className="fi-admin-modal-textarea"
                rows={3}
                value={text}
                onChange={(e) => setText(e.target.value)}
                maxLength={500}
              />
            </label>
          </>
        ) : null}
        {kind === 'unfreeze' ? (
          <p className="fi-admin-modal-lead">{t('totalUsersPage.directory.modalUnfreezeLead')}</p>
        ) : null}
        {kind === 'delete' ? (
          <>
            <p className="fi-admin-modal-lead">
              {t('totalUsersPage.directory.modalDeleteLead', { email: user.email })}
            </p>
            <p className="fi-admin-modal-email" aria-label={t('totalUsersPage.directory.colEmail')}>
              {user.email}
            </p>
            <label className="fi-admin-dir-field fi-admin-modal-check">
              <input
                type="checkbox"
                checked={blockEmail}
                onChange={(e) => setBlockEmail(e.target.checked)}
              />
              <span>{t('totalUsersPage.directory.modalDeleteBlockEmail')}</span>
            </label>
          </>
        ) : null}
        {error ? <PortalAlert variant="error">{error}</PortalAlert> : null}
        <div className="fi-admin-modal-actions">
          <button type="button" className="fi-admin-dir-btn" onClick={onClose} disabled={busy}>
            {t('totalUsersPage.directory.modalCancel')}
          </button>
          <button
            type="button"
            className={`fi-admin-dir-btn fi-admin-dir-btn--primary${kind === 'freeze' || kind === 'delete' ? ' fi-admin-dir-btn--danger' : ''}`}
            disabled={busy || (kind === 'message' && !text.trim())}
            onClick={() => void handleSubmit()}
          >
            {busy
              ? t('totalUsersPage.directory.modalSending')
              : kind === 'message'
                ? t('totalUsersPage.directory.actionSend')
                : kind === 'freeze'
                  ? t('totalUsersPage.directory.actionFreeze')
                  : kind === 'delete'
                    ? t('totalUsersPage.directory.actionDeleteConfirm')
                    : t('totalUsersPage.directory.actionUnfreeze')}
          </button>
        </div>
      </div>
    </div>
  )
}
