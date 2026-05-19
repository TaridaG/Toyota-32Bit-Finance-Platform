import { useInfoCards } from '../info-cards/InfoCardsProvider'
import { InfoCardEditorDrawer } from '../../pages/bilgi-kartlari/components/InfoCardEditorDrawer'
import type { InfoCardInput } from '../../types/infoCards'
import { useAdminInfoCardPick } from './AdminInfoCardPickContext'

export function AdminInfoCardPickEditor() {
  const { editorOpen, pickPrefill, closeEditor } = useAdminInfoCardPick()
  const { createCard } = useInfoCards()

  const handleSave = async (input: InfoCardInput) => {
    await createCard(input)
    closeEditor()
  }

  if (!editorOpen || !pickPrefill) {
    return null
  }

  return (
    <InfoCardEditorDrawer
      open={editorOpen}
      initial={null}
      defaultPage={pickPrefill.pageKey}
      pickPrefill={{
        title: pickPrefill.title,
        targetTerms: pickPrefill.targetTerms,
        targetElementIds: pickPrefill.targetElementIds,
        targetInstrumentSymbols: pickPrefill.targetInstrumentSymbols,
        pages: [pickPrefill.pageKey],
        pickLabel: pickPrefill.pickLabel,
        localeTitles: pickPrefill.localeTitles,
      }}
      onClose={closeEditor}
      onSave={handleSave}
    />
  )
}
