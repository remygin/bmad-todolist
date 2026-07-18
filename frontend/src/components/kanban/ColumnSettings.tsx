import { useState, type FormEvent } from 'react'
import type { BoardColumn, ColumnConfiguration } from '../../api/kanban'

type ColumnSettingsProps = {
  columns: BoardColumn[]
  onSave: (columns: ColumnConfiguration[]) => Promise<void>
  onClose: () => void
}

export function ColumnSettings({ columns, onSave, onClose }: ColumnSettingsProps) {
  const [draft, setDraft] = useState(
    columns.map(({ id, status, name, position }) => ({ id, status, name, position })),
  )
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  function move(index: number, direction: -1 | 1) {
    const target = index + direction
    if (target < 0 || target >= draft.length) return
    const reordered = [...draft]
    ;[reordered[index], reordered[target]] = [reordered[target], reordered[index]]
    setDraft(reordered.map((column, position) => ({ ...column, position })))
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (draft.some((column) => !column.name.trim())) {
      setError('Названия колонок не могут быть пустыми')
      return
    }
    setSaving(true)
    setError('')
    try {
      await onSave(draft.map((column) => ({ ...column, name: column.name.trim() })))
      onClose()
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Не удалось сохранить колонки')
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="column-settings" onSubmit={submit}>
      <div className="settings-heading">
        <h2>Настройка колонок</h2>
        <button type="button" className="button-quiet" onClick={onClose}>
          Закрыть
        </button>
      </div>
      <p className="muted">Системные статусы фиксированы, но названия и порядок можно изменить.</p>
      <div className="settings-list">
        {draft.map((column, index) => (
          <div className="settings-row" key={column.id}>
            <label>
              <span>{column.status.replace('_', ' ')}</span>
              <input
                value={column.name}
                maxLength={80}
                onChange={(event) =>
                  setDraft((current) =>
                    current.map((item) =>
                      item.id === column.id ? { ...item, name: event.target.value } : item,
                    ),
                  )
                }
              />
            </label>
            <div className="order-buttons">
              <button
                type="button"
                className="button-quiet"
                aria-label={`Переместить ${column.name} влево`}
                disabled={index === 0}
                onClick={() => move(index, -1)}
              >
                ←
              </button>
              <button
                type="button"
                className="button-quiet"
                aria-label={`Переместить ${column.name} вправо`}
                disabled={index === draft.length - 1}
                onClick={() => move(index, 1)}
              >
                →
              </button>
            </div>
          </div>
        ))}
      </div>
      {error && <p className="form-error">{error}</p>}
      <button type="submit" disabled={saving}>
        {saving ? 'Сохраняем…' : 'Сохранить настройки'}
      </button>
    </form>
  )
}
