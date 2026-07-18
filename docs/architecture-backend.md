# Архитектура — Backend

## Краткое резюме

REST API Kanban-досок с JWT-аутентификацией администратора. Слоистая структура: Controllers → Services → JPA Repositories → PostgreSQL. Схема версионируется Flyway.

## Стек

| Категория | Технология | Версия |
|-----------|------------|--------|
| Язык | Java | 17 |
| Framework | Spring Boot | 4.0.7 |
| Security | Spring Security + JJWT | 0.12.6 |
| Persistence | Spring Data JPA + Flyway | — |
| БД | PostgreSQL / H2 (test) | 16 / — |
| Тесты | JUnit 5 + MockMvc | — |

## Паттерн архитектуры

**Layered / service-centric API:**
- `*Controller` — HTTP, `@PreAuthorize`
- `*Service` — бизнес-логика, транзакции
- `*Repository` — Spring Data JPA
- `config` — Security filter chain, bootstrap admin
- `common` — единый error envelope

## Данные

См. [data-models-backend.md](./data-models-backend.md). Инварианты: ровно 3 колонки со статусами TODO/IN_PROGRESS/DONE; уникальность имени доски в рамках автора; нормализация `position` карточек.

## API

См. [api-contracts-backend.md](./api-contracts-backend.md). Stateless JWT; CORS `*`; CSRF отключён.

## Безопасность

- Public: `POST /api/auth/login`, `GET /actuator/health`
- Остальной `/api/**` — authenticated
- Kanban — `hasRole('ADMIN')`
- Пароли: BCrypt; JWT secret из env

## Дерево

См. [source-tree-analysis.md](./source-tree-analysis.md) (часть backend/).

## Разработка

```bash
cd backend
SERVER_PORT=8081 mvn spring-boot:run
mvn test   # профиль test, H2
```

## Развёртывание

Docker multi-stage (Maven → Temurin JRE). В Compose зависит от healthy PostgreSQL. Порт контейнера 8080.

## Тестирование

- `AuthIntegrationTest` — login / me / security
- `KanbanIntegrationTest` — CRUD досок/карточек/move
- `TodolistApplicationTests` — контекст
