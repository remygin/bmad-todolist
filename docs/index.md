# Индекс документации проекта

**Проект:** BMAD Todo List  
**Дата:** 2026-07-22  
**Режим сканирования:** deep / full_rescan

## Обзор проекта

- **Тип:** multi-part (3 части)
- **Языки:** TypeScript (frontend), Java (backend + autotests)
- **Архитектура:** REST SPA + layered Spring API + PostgreSQL + E2E harness

### Быстрый справочник

#### frontend (web)

- **Стек:** React 18, TypeScript 5.6, Vite 5, @dnd-kit, react-router
- **Корень:** `frontend/`
- **Точка входа:** `frontend/src/main.tsx`

#### backend (backend)

- **Стек:** Java 17, Spring Boot 4, Security, JPA, Flyway, JWT
- **Корень:** `backend/`
- **Точка входа:** `TodolistApplication.java`
- **Тесты кода:** `backend/src/test` (JUnit + MockMvc + H2)

#### autotests (library)

- **Стек:** Java 17, JUnit 5, Rest Assured, Selenide
- **Корень:** `autotests/`
- **Запуск:** `cd autotests && mvn test` (нужен поднятый стенд)
- **Архитектура:** [architecture-autotests.md](./architecture-autotests.md)

## Сгенерированная документация

- [Обзор проекта](./project-overview.md)
- [Архитектура — Frontend](./architecture-frontend.md)
- [Архитектура — Backend](./architecture-backend.md)
- [Архитектура — Autotests](./architecture-autotests.md)
- [Анализ дерева исходников](./source-tree-analysis.md)
- [Инвентарь компонентов — Frontend](./component-inventory-frontend.md)
- [Руководство по разработке](./development-guide.md)
- [Руководство по развёртыванию](./deployment-guide.md)
- [API-контракты — Backend](./api-contracts-backend.md)
- [Модели данных — Backend](./data-models-backend.md)
- [Архитектура интеграции](./integration-architecture.md)
- [Метаданные частей](./project-parts.json)

## Существующая документация

- [README проекта](../README.md) — быстрый старт, стек, unit/IT и E2E
- [frontend/README.md](../frontend/README.md) — шаблон Vite (мало релевантно продукту)

## С чего начать

1. Прочитать [project-overview.md](./project-overview.md)
2. Локальный запуск — [development-guide.md](./development-guide.md) или корневой README
3. Для UI-фич — [architecture-frontend.md](./architecture-frontend.md) + [component-inventory-frontend.md](./component-inventory-frontend.md)
4. Для API-фич — [architecture-backend.md](./architecture-backend.md) + [api-contracts-backend.md](./api-contracts-backend.md) + [data-models-backend.md](./data-models-backend.md)
5. Для E2E — [architecture-autotests.md](./architecture-autotests.md)
6. Для full-stack — architecture частей + [integration-architecture.md](./integration-architecture.md)
