import { useState } from 'react'
import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import type { Card } from '../../api/kanban'
import { CardForm } from './CardForm'

type KanbanCardProps = {
  card: Card
  onUpdate: (id: number, title: string, description: string) => Promise<void>
  onDelete: (id: number) => Promise<void>
}

export function KanbanCard({ card, onUpdate, onDelete }: KanbanCardProps) {
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: `card:${card.id}`,
    data: { type: 'card', cardId: card.id, status: card.status },
    disabled: editing,
  })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  }

  async function handleDelete() {
    if (!window.confirm(`Удалить карточку «${card.title}»?`)) return
    setBusy(true)
    setError('')
    try {
      await onDelete(card.id)
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Не удалось удалить карточку')
      setBusy(false)
    }
  }

  return (
    <article
      ref={setNodeRef}
      style={style}
      className={`kanban-card${isDragging ? ' is-dragging' : ''}`}
    >
      {editing ? (
        <CardForm
          initialTitle={card.title}
          initialDescription={card.description ?? ''}
          submitLabel="Сохранить"
          busy={busy}
          onCancel={() => setEditing(false)}
          onSubmit={async (title, description) => {
            setBusy(true)
            try {
              await onUpdate(card.id, title, description)
              setEditing(false)
            } finally {
              setBusy(false)
            }
          }}
        />
      ) : (
        <>
          <div className="card-heading">
            <h3>{card.title}</h3>
            <button
              type="button"
              className="drag-handle"
              aria-label={`Переместить карточку ${card.title}`}
              title="Перетащить карточку"
              {...attributes}
              {...listeners}
            >
              ⋮⋮
            </button>
          </div>
          {card.description && <p>{card.description}</p>}
          {error && <p className="form-error">{error}</p>}
          <div className="card-actions">
            <button type="button" className="button-link" onClick={() => setEditing(true)}>
              Изменить
            </button>
            <button type="button" className="button-link danger" onClick={handleDelete} disabled={busy}>
              Удалить
            </button>
          </div>
        </>
      )}
    </article>
  )
}
