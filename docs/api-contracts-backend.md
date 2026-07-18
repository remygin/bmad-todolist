# API-контракты — Backend

**Часть:** backend  
**Базовый путь:** `/api`  
**Аутентификация:** JWT Bearer (`Authorization: Bearer <token>`)  
**Роли Kanban:** `ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`)

## Публичные endpoint

| Метод | Путь | Описание | Auth |
|-------|------|----------|------|
| `POST` | `/api/auth/login` | Вход, выдача JWT | нет |
| `GET` | `/actuator/health` | Health check (Actuator) | нет |

### `POST /api/auth/login`

**Request:**
```json
{ "username": "admin", "password": "admin123" }
```

**Response `200`:**
```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "user": { "id": 1, "username": "admin", "roles": ["ADMIN"] }
}
```

## Защищённые endpoint (JWT)

| Метод | Путь | Описание | Роль |
|-------|------|----------|------|
| `GET` | `/api/auth/me` | Текущий пользователь | authenticated |
| `GET` | `/api/boards` | Список досок автора | ADMIN |
| `GET` | `/api/boards/{id}` | Доска с колонками и карточками | ADMIN |
| `POST` | `/api/boards` | Создать доску | ADMIN |
| `PUT` | `/api/boards/{id}` | Переименовать доску | ADMIN |
| `PUT` | `/api/boards/{id}/columns` | Сохранить набор из 3 колонок | ADMIN |
| `DELETE` | `/api/boards/{id}` | Удалить доску (каскад) | ADMIN |
| `POST` | `/api/boards/{boardId}/cards` | Создать карточку | ADMIN |
| `PUT` | `/api/cards/{id}` | Обновить заголовок/описание | ADMIN |
| `PATCH` | `/api/cards/{id}/move` | Переместить карточку | ADMIN |
| `DELETE` | `/api/cards/{id}` | Удалить карточку | ADMIN |

### Доски

**`POST /api/boards`** — body: `{ "name": string }` (max 120) → `201 BoardResponse`

**`PUT /api/boards/{id}`** — body: `{ "name": string }` → `BoardResponse`

**`PUT /api/boards/{id}/columns`** — ровно 3 колонки со статусами `TODO` | `IN_PROGRESS` | `DONE`:
```json
{
  "columns": [
    { "id": 1, "status": "TODO", "name": "К выполнению", "position": 0 },
    { "id": 2, "status": "IN_PROGRESS", "name": "В работе", "position": 1 },
    { "id": 3, "status": "DONE", "name": "Готово", "position": 2 }
  ]
}
```

**`BoardResponse`:** id, name, authorId, authorUsername, createdAt, updatedAt, columns[]  
**`ColumnResponse`:** id, status, name, position, cards[]

### Карточки

**`POST /api/boards/{boardId}/cards`:**
```json
{ "title": string, "description": string | null, "status": "TODO"|"IN_PROGRESS"|"DONE" }
```
→ `201 CardResponse`

**`PUT /api/cards/{id}`:** `{ "title", "description" }` → `CardResponse`

**`PATCH /api/cards/{id}/move`:**
```json
{ "targetStatus": "IN_PROGRESS", "targetIndex": 0 }
```
→ `CardResponse` (позиции нормализуются)

**`CardResponse`:** id, boardId, title, description, status, position, createdAt, updatedAt

## Ошибки

Единый формат через `ApiExceptionHandler` / `ErrorResponse`:
```json
{ "message": string, "details": string[] }
```

Типичные коды: `400` (валидация / BadRequest), `401` (нет/битый JWT), `403` (нет роли), `404`, `409` (конфликт имён досок и т.п.).

## Контроллеры (исходники)

- `auth/AuthController.java`
- `board/BoardController.java`
- `card/CardController.java`
- `config/SecurityConfig.java` — whitelist login + health
