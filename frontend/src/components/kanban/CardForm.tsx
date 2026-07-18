import { useState, type FormEvent } from 'react'

type CardFormProps = {
  initialTitle?: string
  initialDescription?: string
  submitLabel: string
  busy?: boolean
  onSubmit: (title: string, description: string) => Promise<void>
  onCancel?: () => void
}

export function CardForm({
  initialTitle = '',
  initialDescription = '',
  submitLabel,
  busy = false,
  onSubmit,
  onCancel,
}: CardFormProps) {
  const [title, setTitle] = useState(initialTitle)
  const [description, setDescription] = useState(initialDescription)
  const [error, setError] = useState('')

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!title.trim()) {
      setError('Введите заголовок карточки')
      return
    }
    setError('')
    try {
      await onSubmit(title.trim(), description.trim())
      if (!initialTitle) {
        setTitle('')
        setDescription('')
      }
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Не удалось сохранить карточку')
    }
  }

  return (
    <form className="card-form" onSubmit={handleSubmit}>
      <label>
        <span className="sr-only">Заголовок карточки</span>
        <input
          value={title}
          maxLength={200}
          placeholder="Что нужно сделать?"
          onChange={(event) => setTitle(event.target.value)}
          disabled={busy}
          autoFocus={Boolean(initialTitle)}
        />
      </label>
      <label>
        <span className="sr-only">Описание карточки</span>
        <textarea
          value={description}
          maxLength={4000}
          placeholder="Описание (необязательно)"
          onChange={(event) => setDescription(event.target.value)}
          disabled={busy}
          rows={3}
        />
      </label>
      {error && <p className="form-error">{error}</p>}
      <div className="form-actions">
        <button type="submit" disabled={busy}>
          {submitLabel}
        </button>
        {onCancel && (
          <button type="button" className="button-quiet" onClick={onCancel} disabled={busy}>
            Отмена
          </button>
        )}
      </div>
    </form>
  )
}
