import { useEffect, useState, type FormEvent } from 'react'
import {
  configureColumns,
  createBoard,
  createCard,
  deleteBoard,
  deleteCard,
  getBoard,
  listBoards,
  moveCard,
  renameBoard,
  updateCard,
  type Board,
  type BoardSummary,
  type ColumnConfiguration,
  type ColumnStatus,
} from '../api/kanban'
import { ColumnSettings } from '../components/kanban/ColumnSettings'
import { KanbanBoard } from '../components/kanban/KanbanBoard'

export function BoardsPage() {
  const [boards, setBoards] = useState<BoardSummary[]>([])
  const [board, setBoard] = useState<Board | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [newBoardName, setNewBoardName] = useState('')
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    listBoards()
      .then(async (items) => {
        if (!active) return
        setBoards(items)
        if (items.length > 0) {
          const selected = await getBoard(items[0].id)
          if (active) setBoard(selected)
        }
      })
      .catch((caught) => {
        if (active) setError(errorMessage(caught))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  async function selectBoard(id: number) {
    setLoading(true)
    setError('')
    setSettingsOpen(false)
    try {
      setBoard(await getBoard(id))
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setLoading(false)
    }
  }

  async function handleCreateBoard(event: FormEvent) {
    event.preventDefault()
    if (!newBoardName.trim()) return
    setBusy(true)
    setError('')
    try {
      const created = await createBoard(newBoardName.trim())
      setBoards((current) => [...current, summaryFromBoard(created)])
      setBoard(created)
      setNewBoardName('')
      setSettingsOpen(false)
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function handleRenameBoard() {
    if (!board) return
    const name = window.prompt('Новое название доски', board.name)
    if (!name?.trim() || name.trim() === board.name) return
    setBusy(true)
    setError('')
    try {
      const updated = await renameBoard(board.id, name.trim())
      setBoard(updated)
      setBoards((current) =>
        current.map((item) => (item.id === updated.id ? summaryFromBoard(updated) : item)),
      )
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function handleDeleteBoard() {
    if (!board || !window.confirm(`Удалить доску «${board.name}» и все её карточки?`)) {
      return
    }
    setBusy(true)
    setError('')
    try {
      await deleteBoard(board.id)
      const remaining = boards.filter((item) => item.id !== board.id)
      setBoards(remaining)
      setSettingsOpen(false)
      setBoard(remaining.length > 0 ? await getBoard(remaining[0].id) : null)
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function handleConfigureColumns(columns: ColumnConfiguration[]) {
    if (!board) return
    const updated = await configureColumns(board.id, columns)
    setBoard(updated)
  }

  async function handleCreateCard(
    status: ColumnStatus,
    title: string,
    description: string,
  ) {
    if (!board) return
    const created = await createCard(board.id, { title, description, status })
    setBoard((current) =>
      current
        ? {
            ...current,
            columns: current.columns.map((column) =>
              column.status === status
                ? { ...column, cards: [...column.cards, created] }
                : column,
            ),
          }
        : current,
    )
  }

  async function handleUpdateCard(id: number, title: string, description: string) {
    const updated = await updateCard(id, { title, description })
    setBoard((current) =>
      current
        ? {
            ...current,
            columns: current.columns.map((column) => ({
              ...column,
              cards: column.cards.map((card) => (card.id === id ? updated : card)),
            })),
          }
        : current,
    )
  }

  async function handleDeleteCard(id: number) {
    await deleteCard(id)
    setBoard((current) =>
      current
        ? {
            ...current,
            columns: current.columns.map((column) => ({
              ...column,
              cards: column.cards
                .filter((card) => card.id !== id)
                .map((card, position) => ({ ...card, position })),
            })),
          }
        : current,
    )
  }

  async function handleMoveCard(
    id: number,
    targetStatus: ColumnStatus,
    targetIndex: number,
  ) {
    if (!board) return
    const previous = board
    setError('')
    setBoard(moveCardLocally(board, id, targetStatus, targetIndex))
    try {
      await moveCard(id, targetStatus, targetIndex)
    } catch (caught) {
      setBoard(previous)
      setError(`Перемещение отменено: ${errorMessage(caught)}`)
    }
  }

  if (loading && !board) {
    return <div className="board-state">Загружаем доски…</div>
  }

  return (
    <div className="boards-page">
      <aside className="board-sidebar">
        <div>
          <h2>Доски</h2>
          <p className="muted">Ваши рабочие пространства</p>
        </div>
        <nav className="board-list" aria-label="Список досок">
          {boards.map((item) => (
            <button
              type="button"
              key={item.id}
              className={item.id === board?.id ? 'active' : ''}
              onClick={() => void selectBoard(item.id)}
            >
              {item.name}
            </button>
          ))}
        </nav>
        <form className="new-board-form" onSubmit={handleCreateBoard}>
          <label>
            <span>Новая доска</span>
            <input
              value={newBoardName}
              maxLength={120}
              placeholder="Название"
              onChange={(event) => setNewBoardName(event.target.value)}
              disabled={busy}
            />
          </label>
          <button type="submit" disabled={busy || !newBoardName.trim()}>
            Создать
          </button>
        </form>
      </aside>

      <section className="board-workspace">
        {error && (
          <div className="error board-error" role="alert">
            {error}
          </div>
        )}
        {board ? (
          <>
            <header className="board-toolbar">
              <div>
                <p className="eyebrow">Kanban-доска</p>
                <h1>{board.name}</h1>
              </div>
              <div className="toolbar-actions">
                <button
                  type="button"
                  className="button-quiet"
                  onClick={() => setSettingsOpen((open) => !open)}
                >
                  Колонки
                </button>
                <button type="button" className="button-quiet" onClick={handleRenameBoard} disabled={busy}>
                  Переименовать
                </button>
                <button type="button" className="button-danger" onClick={handleDeleteBoard} disabled={busy}>
                  Удалить
                </button>
              </div>
            </header>
            {settingsOpen && (
              <ColumnSettings
                key={board.columns.map((column) => `${column.id}:${column.position}`).join(',')}
                columns={board.columns}
                onSave={handleConfigureColumns}
                onClose={() => setSettingsOpen(false)}
              />
            )}
            {loading ? (
              <div className="board-state">Загружаем доску…</div>
            ) : (
              <KanbanBoard
                board={board}
                onCreateCard={handleCreateCard}
                onUpdateCard={handleUpdateCard}
                onDeleteCard={handleDeleteCard}
                onMoveCard={handleMoveCard}
              />
            )}
          </>
        ) : (
          <div className="empty-state">
            <div className="empty-icon">▦</div>
            <h1>Создайте первую доску</h1>
            <p>Добавьте доску слева, чтобы начать вести задачи по Kanban.</p>
          </div>
        )}
      </section>
    </div>
  )
}

function summaryFromBoard(board: Board): BoardSummary {
  return {
    id: board.id,
    name: board.name,
    authorId: board.authorId,
    authorUsername: board.authorUsername,
    createdAt: board.createdAt,
    updatedAt: board.updatedAt,
  }
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Не удалось выполнить действие'
}

function moveCardLocally(
  board: Board,
  cardId: number,
  targetStatus: ColumnStatus,
  targetIndex: number,
): Board {
  const columns = board.columns.map((column) => ({
    ...column,
    cards: column.cards.map((card) => ({ ...card })),
  }))
  let moving = columns.flatMap((column) => column.cards).find((card) => card.id === cardId)
  if (!moving) return board

  for (const column of columns) {
    column.cards = column.cards.filter((card) => card.id !== cardId)
    column.cards.forEach((card, position) => {
      card.position = position
    })
  }
  const target = columns.find((column) => column.status === targetStatus)
  if (!target) return board

  moving = { ...moving, status: targetStatus }
  target.cards.splice(Math.min(targetIndex, target.cards.length), 0, moving)
  target.cards.forEach((card, position) => {
    card.position = position
    card.status = targetStatus
  })
  return { ...board, columns }
}
