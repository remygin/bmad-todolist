import {
  DndContext,
  KeyboardSensor,
  PointerSensor,
  closestCorners,
  useSensor,
  useSensors,
  type DragEndEvent,
} from '@dnd-kit/core'
import { sortableKeyboardCoordinates } from '@dnd-kit/sortable'
import type { Board, ColumnStatus } from '../../api/kanban'
import { KanbanColumn } from './KanbanColumn'

type KanbanBoardProps = {
  board: Board
  onCreateCard: (
    status: ColumnStatus,
    title: string,
    description: string,
  ) => Promise<void>
  onUpdateCard: (id: number, title: string, description: string) => Promise<void>
  onDeleteCard: (id: number) => Promise<void>
  onMoveCard: (
    cardId: number,
    targetStatus: ColumnStatus,
    targetIndex: number,
  ) => Promise<void>
}

export function KanbanBoard({
  board,
  onCreateCard,
  onUpdateCard,
  onDeleteCard,
  onMoveCard,
}: KanbanBoardProps) {
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event
    if (!over || active.id === over.id) return

    const cardId = Number(String(active.id).replace('card:', ''))
    const target = String(over.id)
    let targetStatus: ColumnStatus | undefined
    let targetIndex = 0

    if (target.startsWith('column:')) {
      targetStatus = target.replace('column:', '') as ColumnStatus
      targetIndex =
        board.columns.find((column) => column.status === targetStatus)?.cards.length ?? 0
    } else {
      const overCardId = Number(target.replace('card:', ''))
      const targetColumn = board.columns.find((column) =>
        column.cards.some((card) => card.id === overCardId),
      )
      if (targetColumn) {
        targetStatus = targetColumn.status
        targetIndex = targetColumn.cards.findIndex((card) => card.id === overCardId)
      }
    }

    if (targetStatus) {
      void onMoveCard(cardId, targetStatus, targetIndex)
    }
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragEnd={handleDragEnd}
    >
      <div className="kanban-board">
        {board.columns.map((column) => (
          <KanbanColumn
            key={column.id}
            column={column}
            onCreate={(title, description) =>
              onCreateCard(column.status, title, description)
            }
            onUpdate={onUpdateCard}
            onDelete={onDeleteCard}
          />
        ))}
      </div>
    </DndContext>
  )
}
