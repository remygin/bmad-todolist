# Обзор проекта — BMAD Todo List

**Дата документации:** 2026-07-22  
**Тип репозитория:** multi-part (3 части)  
**Назначение:** Kanban-приложение с JWT-админом, несколькими досками, настраиваемыми колонками и drag-and-drop карточек.

## Краткое резюме

Full-stack todo/kanban: Spring Boot API + React SPA + PostgreSQL. Один администратор (роль `ADMIN`), фиксированные системные статусы колонок `TODO` / `IN_PROGRESS` / `DONE` с настраиваемыми отображаемыми именами и порядком. E2E-автотесты вынесены в отдельный каталог `autotests/`.

## Стек

| Часть | Технологии |
|-------|------------|
| backend | Java 17, Spring Boot 4, Security, JPA, Flyway, JWT, PostgreSQL |
| frontend | React 18, TypeScript, Vite 5, react-router, @dnd-kit |
| autotests | Java 17, JUnit 5, Rest Assured (API), Selenide (UI) |
| ops | Docker Compose, nginx (prod frontend), Actuator health |

## Архитектура

- **backend:** layered REST API, stateless JWT; unit/IT в `backend/src/test`
- **frontend:** component SPA + Context auth; gate lint+build
- **autotests:** E2E harness против поднятого стенда
- **связь:** REST JSON через `/api` (см. [integration-architecture.md](./integration-architecture.md))

## Части

| ID | Тип | Корень |
|----|-----|--------|
| frontend | web | `frontend/` |
| backend | backend | `backend/` |
| autotests | library | `autotests/` |

## Навигация по документации

- [Index](./index.md) — главная точка входа для AI
- [Архитектура backend](./architecture-backend.md) / [frontend](./architecture-frontend.md) / [autotests](./architecture-autotests.md)
- [API](./api-contracts-backend.md) · [Модели данных](./data-models-backend.md)
- [Дерево](./source-tree-analysis.md) · [Интеграция](./integration-architecture.md)
- [Разработка](./development-guide.md) · [Развёртывание](./deployment-guide.md)
- [Компоненты UI](./component-inventory-frontend.md)

## Существующая документация

- [../README.md](../README.md) — быстрый старт и краткий API
