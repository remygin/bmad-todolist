# Руководство по разработке

## Предварительные требования

- Java 17+
- Maven (или `./mvnw` в `backend/`)
- Node.js 20+ / npm
- PostgreSQL 16 (или Docker только для `db`)

## Окружение

```bash
cp .env.example .env
```

Ключевые переменные: `DATABASE_*`, `JWT_*`, `ADMIN_*`, порты.

## Локальный запуск

### БД

```bash
docker compose up -d db
# или свой PostgreSQL на :5432 с БД/пользователем из .env
```

### Backend

```bash
cd backend
SERVER_PORT=8081 mvn spring-boot:run
```

API: `http://localhost:8081`  
Health: `http://localhost:8081/actuator/health`  
Логин по умолчанию: `admin` / `admin123`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173` (проксирует `/api` → `:8081`)

## Команды

| Часть | Команда | Назначение |
|-------|---------|------------|
| backend | `mvn spring-boot:run` | Dev-сервер |
| backend | `mvn test` | Интеграционные тесты (H2) |
| frontend | `npm run dev` | Vite HMR |
| frontend | `npm run lint` | ESLint |
| frontend | `npm run build` | `tsc -b && vite build` |
| frontend | `npm run preview` | Просмотр production-сборки |

## Структура работы

- Backend-пакеты по домену: `auth`, `board`, `card`, `user`
- Frontend: страницы → kanban-компоненты → `api/*`
- Миграции только через Flyway (`V*__*.sql`); `ddl-auto=validate`

## Типичные задачи

1. **Новый API endpoint** — controller + service + DTO + тест MockMvc; зеркало в `frontend/src/api`
2. **Изменение схемы** — новая Flyway-миграция + JPA entity
3. **UI-карточки** — правки в `components/kanban/*`

См. также корневой [README.md](../README.md).
