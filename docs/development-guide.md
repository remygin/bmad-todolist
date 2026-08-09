# Руководство по разработке

## Предварительные требования

- Java 17+
- Maven (или `./mvnw` в `backend/` / `autotests/`)
- Node.js 20+ / npm
- PostgreSQL 16 (или Docker только для `db`)
- Chrome (для UI E2E в `autotests`; Selenide headless по умолчанию)

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
| backend | `mvn test` | Unit / интеграционные тесты (H2) |
| frontend | `npm run dev` | Vite HMR |
| frontend | `npm run lint` | ESLint |
| frontend | `npm run build` | `tsc -b && vite build` |
| frontend | `npm run preview` | Просмотр production-сборки |
| autotests | `mvn test` | E2E API + UI (нужен поднятый стенд) |

## Автотесты: где что писать

| Что тестируете | Куда |
|----------------|------|
| Логика сервиса / HTTP-контракт in-process | `backend/src/test` (MockMvc IT) |
| FE статическая проверка | `frontend` → lint + build |
| Сценарий против живого API/UI | `autotests/` (Rest Assured / Selenide) |

Конфиг E2E: `autotests/src/test/resources/autotest.properties`  
Переопределение: `API_BASE_URL`, `UI_BASE_URL`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`.

Если стенд недоступен, smoke в `autotests` пропускаются (`assumeTrue`).

## Структура работы

- Backend-пакеты по домену: `auth`, `board`, `card`, `user`
- Frontend: страницы → kanban-компоненты → `api/*`
- Миграции только через Flyway (`V*__*.sql`); `ddl-auto=validate`
- E2E: Page Objects в `autotests/.../ui/pages`, API-хелперы в `.../api`

## Типичные задачи

1. **Новый API endpoint** — controller + service + DTO + MockMvc IT; зеркало в `frontend/src/api`; при необходимости E2E в `autotests`
2. **Изменение схемы** — новая Flyway-миграция + JPA entity
3. **UI-карточки** — правки в `components/kanban/*`; UI smoke/сценарии — в `autotests`

См. также корневой [README.md](../README.md) и [architecture-autotests.md](./architecture-autotests.md).
