import { useAuth } from '../auth/useAuth'
import { BoardsPage } from './BoardsPage'

export function AdminShellPage() {
  const { user, logout } = useAuth()

  return (
    <div className="shell">
      <header className="shell-header">
        <div>
          <div className="brand-mark" aria-hidden="true">B</div>
          <div>
            <h1>BMAD Kanban</h1>
            <p className="muted">Планируйте. Делайте. Завершайте.</p>
          </div>
        </div>
        <div className="shell-user">
          <span>
            {user?.username} ({user?.roles.join(', ')})
          </span>
          <button type="button" onClick={logout}>
            Выйти
          </button>
        </div>
      </header>
      <main className="shell-main">
        <BoardsPage />
      </main>
    </div>
  )
}
