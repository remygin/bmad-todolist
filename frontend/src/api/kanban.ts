import { apiRequest } from './client'

export type ColumnStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'

export type BoardSummary = {
  id: number
  name: string
  authorId: number
  authorUsername: string
  createdAt: string
  updatedAt: string
}

export type Card = {
  id: number
  boardId: number
  title: string
  description: string | null
  status: ColumnStatus
  position: number
  createdAt: string
  updatedAt: string
}

export type BoardColumn = {
  id: number
  status: ColumnStatus
  name: string
  position: number
  cards: Card[]
}

export type Board = BoardSummary & {
  columns: BoardColumn[]
}

export type ColumnConfiguration = {
  id: number
  status: ColumnStatus
  name: string
  position: number
}

export function listBoards(): Promise<BoardSummary[]> {
  return apiRequest('/boards')
}

export function getBoard(id: number): Promise<Board> {
  return apiRequest(`/boards/${id}`)
}

export function createBoard(name: string): Promise<Board> {
  return apiRequest('/boards', {
    method: 'POST',
    body: JSON.stringify({ name }),
  })
}

export function renameBoard(id: number, name: string): Promise<Board> {
  return apiRequest(`/boards/${id}`, {
    method: 'PUT',
    body: JSON.stringify({ name }),
  })
}

export function deleteBoard(id: number): Promise<void> {
  return apiRequest(`/boards/${id}`, { method: 'DELETE' })
}

export function configureColumns(
  boardId: number,
  columns: ColumnConfiguration[],
): Promise<Board> {
  return apiRequest(`/boards/${boardId}/columns`, {
    method: 'PUT',
    body: JSON.stringify({ columns }),
  })
}

export function createCard(
  boardId: number,
  input: { title: string; description: string; status: ColumnStatus },
): Promise<Card> {
  return apiRequest(`/boards/${boardId}/cards`, {
    method: 'POST',
    body: JSON.stringify(input),
  })
}

export function updateCard(
  id: number,
  input: { title: string; description: string },
): Promise<Card> {
  return apiRequest(`/cards/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
}

export function moveCard(
  id: number,
  targetStatus: ColumnStatus,
  targetIndex: number,
): Promise<Card> {
  return apiRequest(`/cards/${id}/move`, {
    method: 'PATCH',
    body: JSON.stringify({ targetStatus, targetIndex }),
  })
}

export function deleteCard(id: number): Promise<void> {
  return apiRequest(`/cards/${id}`, { method: 'DELETE' })
}
