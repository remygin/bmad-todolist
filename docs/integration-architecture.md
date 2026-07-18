# Архитектура интеграции

## Обзор

```
Browser → Frontend (Vite/nginx) → Backend (Spring Boot) → PostgreSQL
              │                         │
         /api proxy              JWT + JPA/Flyway
```

## Точки интеграции

| From | To | Тип | Детали |
|------|-----|-----|--------|
| frontend | backend | REST JSON | `fetch('/api...')` + `Authorization: Bearer` |
| frontend (dev) | backend :8081 | Vite proxy | `vite.config.ts` — `/api`, `/actuator` |
| frontend (Docker) | backend:8080 | nginx proxy | `nginx.conf` — `/api/`, `/actuator/health` |
| backend | PostgreSQL | JDBC/JPA | `DATABASE_URL`, Flyway migrations |
| docker-compose | all | orchestration | healthcheck db → backend → frontend |

## Поток аутентификации

1. `POST /api/auth/login` → JWT в `sessionStorage` (`todolist.accessToken`)
2. Клиент добавляет `Authorization: Bearer` через `api/client.ts`
3. `JwtAuthenticationFilter` валидирует токен; Kanban требует роль `ADMIN`
4. `GET /api/auth/me` при загрузке SPA восстанавливает сессию

## Поток данных Kanban

1. UI вызывает `api/kanban.ts` (list/get/create/rename/delete boards, columns, cards, move)
2. Backend сервисы (`BoardService`, `CardService`) владеют бизнес-правилами (3 колонки, нормализация позиций)
3. Удаление доски каскадно удаляет колонки и карточки (FK ON DELETE CASCADE)

## Общие контракты

Типы зеркалятся между `frontend/src/api/kanban.ts` и Java DTO (`BoardDtos`, `CardDtos`). Статусы: `TODO` | `IN_PROGRESS` | `DONE`.
