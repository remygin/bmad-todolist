import { useState } from 'react'
import { useDroppable } from '@dnd-kit/core'
import {
  SortableContext,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import type { BoardColumn } from '../../api/kanban'
import { CardForm } from './CardForm'
import { KanbanCard } from './KanbanCard'

type KanbanColumnProps = {
  column: BoardColumn
  onCreate: (title: string, description: string) => Promise<void>
  onUpdate: (id: number, title: string, description: string) => Promise<void>
  onDelete: (id: number) => Promise<void>
}

export function KanbanColumn({
  column,
  onCreate,
  onUpdate,
  onDelete,
}: KanbanColumnProps) {
  const [creating, setCreating] = useState(false)
  const { setNodeRef, isOver } = useDroppable({
    id: `column:${column.status}`,
    data: { type: 'column', status: column.status },
  })

  return (
    <section
      ref={setNodeRef}
      className={`kanban-column${isOver ? ' is-over' : ''}`}
      aria-label={column.name}
    >
      <header className="column-header">
        <h2>{column.name}</h2>
        <span className="count-badge" aria-label={`${column.cards.length} карточек`}>
          {column.cards.length}
        </span>
      </header>
      <SortableContext
        items={column.cards.map((card) => `card:${card.id}`)}
        strategy={verticalListSortingStrategy}
      >
        <div className="card-list">
          {column.cards.map((card) => (
            <KanbanCard
              key={card.id}
              card={card}
              onUpdate={onUpdate}
              onDelete={onDelete}
            />
          ))}
          {column.cards.length === 0 && (
            <p className="column-empty">Перетащите карточку сюда</p>
          )}
        </div>
      </SortableContext>
      {creating ? (
        <CardForm
          submitLabel="Добавить"
          onCancel={() => setCreating(false)}
          onSubmit={async (title, description) => {
            await onCreate(title, description)
            setCreating(false)
          }}
        />
      ) : (
        <button type="button" className="add-card-button" onClick={() => setCreating(true)}>
          + Добавить карточку
        </button>
      )}
    </section>
  )
}
