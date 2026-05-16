type LiteracyEmptyStateProps = {
  title: string
  message: string
}

export function LiteracyEmptyState({ title, message }: LiteracyEmptyStateProps) {
  return (
    <div className="lit-empty card" role="status">
      <h3>{title}</h3>
      <p>{message}</p>
    </div>
  )
}
