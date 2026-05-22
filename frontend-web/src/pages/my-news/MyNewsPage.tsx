import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { MyNewsPanel } from './MyNewsPanel'

/** Standalone route; portfolio uses {@link MyNewsPanel} embedded with sidebar. */
export function MyNewsPage() {
  const { t } = useTranslation('myNewsPage')
  useDocumentTitle(t('titleDoc'))
  return <MyNewsPanel />
}
