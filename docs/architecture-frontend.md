# Архитектура — Frontend

## Краткое резюме

SPA-админка Kanban: логин по JWT, список досок, колонки с drag-and-drop карточек (`@dnd-kit`). Общается с backend только через `/api` (прокси Vite или nginx).

## Стек

| Категория | Технология | Версия |
|-----------|------------|--------|
| UI | React | 18.3 |
| Язык | TypeScript | ~5.6 |
| Сборка | Vite | 5.4 |
| Роутинг | react-router-dom | 6.30 |
| DnD | @dnd-kit/core, sortable, utilities | 6.x / 10.x |
| State | React Context (`AuthContext`) | — |

## Паттерн архитектуры

**Component-based SPA:**
- `pages/` — маршруты (Login, AdminShell/Boards)
- `components/kanban/` — доска, колонки, карточки, формы, настройки колонок
- `api/` — тонкий HTTP-слой
- `auth/` — провайдер сессии + `ProtectedRoute`

## Управление состоянием

- Auth: Context + `sessionStorage` для JWT
- Данные доски: локальный state на страницах/компонентах после API-вызовов (без Redux)

## UI-компоненты

См. [component-inventory-frontend.md](./component-inventory-frontend.md).

## Интеграция с API

`api/client.ts` → `fetch('/api' + path)`. Dev-proxy на `localhost:8081`. Docker: nginx проксирует на `backend:8080`.

## Дерево

См. [source-tree-analysis.md](./source-tree-analysis.md) (часть frontend/).

## Разработка

```bash
cd frontend
npm install
npm run dev      # :5173
npm run lint
npm run build
```

## Развёртывание

Dockerfile: `npm ci` + `vite build` → nginx:1.27 с `nginx.conf`.
