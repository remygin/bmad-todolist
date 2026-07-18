# Инвентарь UI-компонентов — Frontend

## Страницы

| Компонент | Путь | Назначение |
|-----------|------|------------|
| `LoginPage` | `pages/LoginPage.tsx` | Форма входа |
| `AdminShellPage` | `pages/AdminShellPage.tsx` | Оболочка после auth (logout и т.п.) |
| `BoardsPage` | `pages/BoardsPage.tsx` | Список досок + активная доска |

## Auth / маршрутизация

| Компонент | Путь | Назначение |
|-----------|------|------------|
| `AuthProvider` | `auth/AuthContext.tsx` | Сессия, login/logout, bootstrap `/me` |
| `useAuth` | `auth/useAuth.ts` | Хук контекста |
| `ProtectedRoute` | `auth/ProtectedRoute.tsx` | Редирект на `/login` |

## Kanban

| Компонент | Путь | Категория |
|-----------|------|-----------|
| `KanbanBoard` | `components/kanban/KanbanBoard.tsx` | Layout / Display |
| `KanbanColumn` | `components/kanban/KanbanColumn.tsx` | Display + DnD droppable |
| `KanbanCard` | `components/kanban/KanbanCard.tsx` | Display + DnD handle |
| `CardForm` | `components/kanban/CardForm.tsx` | Form (create/edit) |
| `ColumnSettings` | `components/kanban/ColumnSettings.tsx` | Form (rename/reorder 3 cols) |

## API-слой (не UI, но связан)

| Модуль | Путь |
|--------|------|
| HTTP client | `api/client.ts` |
| Auth API | `api/auth.ts` |
| Kanban API | `api/kanban.ts` |

## Design system

Отдельной библиотеки компонентов нет; стили в `App.css` / `index.css`. DnD — `@dnd-kit` (мышь, touch, keyboard через handle).
