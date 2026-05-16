type LearningPathCardProps = {
  title: string
  steps: { order: number; label: string; categoryKey: string }[]
  onStepClick: (categoryKey: string) => void
}

export function LearningPathCard({ title, steps, onStepClick }: LearningPathCardProps) {
  return (
    <aside className="lit-learning-path card">
      <h3>{title}</h3>
      <ol className="lit-learning-path-list">
        {steps.map((step) => (
          <li key={step.categoryKey}>
            <button type="button" className="lit-learning-path-step" onClick={() => onStepClick(step.categoryKey)}>
              <span className="lit-learning-path-num">{step.order}</span>
              <span>{step.label}</span>
            </button>
          </li>
        ))}
      </ol>
    </aside>
  )
}
