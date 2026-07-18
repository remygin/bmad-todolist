# BMAD Todo List

Kanban-приложение на Spring Boot + React с JWT-аутентификацией администратора,
несколькими досками, настраиваемыми колонками и drag-and-drop карточек.

## Стек

- **Backend:** Java 17, Spring Boot 4, Spring Security, JPA, Flyway, JWT
- **Frontend:** React 18 + TypeScript + Vite 5 + `@dnd-kit`
- **БД:** PostgreSQL (приложение), H2 (автотесты)
- **Автотесты:** JUnit 5 + MockMvc

> На локальной машине проекта используется Java 17 (Spring Boot 4 её поддерживает). Docker Compose описан в репозитории; для полного стека нужен установленный Docker.

## Быстрый старт (локально)

### 1. Переменные окружения

```bash
cp .env.example .env
```

### 2. PostgreSQL

Нужен PostgreSQL на `localhost:5432` с БД/пользователем из `.env`, либо:

```bash
docker compose up -d db
```

### 3. Backend

```bash
cd backend
SERVER_PORT=8081 mvn spring-boot:run
```

API: `http://localhost:8081`. Порт `8081` соответствует proxy в
`frontend/vite.config.ts`; при смене порта обновите target proxy.

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173` (проксирует `/api` на backend `:8081`)

Логин по умолчанию: `admin` / `admin123` (меняется через `ADMIN_USERNAME` / `ADMIN_PASSWORD`).

## Docker Compose (весь стек)

```bash
cp .env.example .env
docker compose up --build
```

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`

## Автотесты

```bash
cd backend
mvn test

cd ../frontend
npm run lint
npm run build
```

Тесты используют профиль `test` и in-memory H2 — PostgreSQL не требуется.

## API

Публичные endpoint:

- `POST /api/auth/login` — вход и получение JWT;
- `GET /actuator/health` — health check.

Endpoint с JWT:

- `GET /api/auth/me` — текущий пользователь и роли.

Kanban endpoint доступны только пользователю с ролью `ADMIN`:

- `GET/POST /api/boards` — список и создание досок;
- `GET/PUT/DELETE /api/boards/{id}` — просмотр, переименование и удаление доски;
- `PUT /api/boards/{id}/columns` — атомарное сохранение полного набора трёх колонок;
- `POST /api/boards/{id}/cards` — создание карточки;
- `PUT/DELETE /api/cards/{id}` — изменение и удаление карточки;
- `PATCH /api/cards/{id}/move` — атомарное перемещение карточки.

Новая доска всегда получает системные статусы `TODO`, `IN_PROGRESS`, `DONE`.
Их нельзя добавить, удалить или заменить, но отображаемые названия и порядок
колонок можно менять. Позиции карточек нормализуются после каждой мутации.

Роли хранятся в БД (`roles` + `user_roles`). Сейчас используется только `ADMIN`; схема готова к добавлению новых ролей без миграции структуры.

## Структура

```
backend/     Spring Boot API
frontend/    React UI
docker-compose.yml
.env.example
```

## Работа с интерфейсом

После входа создайте доску в левой панели. Карточки добавляются внутри колонок,
редактируются на месте и перемещаются мышью, касанием или клавиатурой за
drag-handle. Кнопка «Колонки» позволяет переименовать и переставить три
фиксированные колонки. Удаление доски требует подтверждения и каскадно удаляет
её карточки.
