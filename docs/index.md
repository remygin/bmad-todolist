# Индекс документации проекта

**Проект:** BMAD Todo List  
**Дата:** 2026-07-18  
**Режим сканирования:** deep / initial_scan

## Обзор проекта

- **Тип:** multi-part (2 части)
- **Языки:** TypeScript (frontend), Java (backend)
- **Архитектура:** REST SPA + layered Spring API + PostgreSQL

### Быстрый справочник

#### frontend (web)

- **Стек:** React 18, TypeScript 5.6, Vite 5, @dnd-kit, react-router
- **Корень:** `frontend/`
- **Точка входа:** `frontend/src/main.tsx`

#### backend (backend)

- **Стек:** Java 17, Spring Boot 4, Security, JPA, Flyway, JWT
- **Корень:** `backend/`
- **Точка входа:** `TodolistApplication.java`

## Сгенерированная документация

- [Обзор проекта](./project-overview.md)
- [Архитектура — Frontend](./architecture-frontend.md)
- [Архитектура — Backend](./architecture-backend.md)
- [Анализ дерева исходников](./source-tree-analysis.md)
- [Инвентарь компонентов — Frontend](./component-inventory-frontend.md)
- [Руководство по разработке](./development-guide.md)
- [Руководство по развёртыванию](./deployment-guide.md)
- [API-контракты — Backend](./api-contracts-backend.md)
- [Модели данных — Backend](./data-models-backend.md)
- [Архитектура интеграции](./integration-architecture.md)
- [Метаданные частей](./project-parts.json)

## Существующая документация

- [README проекта](../README.md) — быстрый старт, стек, краткий API
- [frontend/README.md](../frontend/README.md) — шаблон Vite (мало релевантно продукту)

## С чего начать

1. Прочитать [project-overview.md](./project-overview.md)
2. Локальный запуск — [development-guide.md](./development-guide.md) или корневой README
3. Для UI-фич — [architecture-frontend.md](./architecture-frontend.md) + [component-inventory-frontend.md](./component-inventory-frontend.md)
4. Для API-фич — [architecture-backend.md](./architecture-backend.md) + [api-contracts-backend.md](./api-contracts-backend.md) + [data-models-backend.md](./data-models-backend.md)
5. Для full-stack — оба architecture + [integration-architecture.md](./integration-architecture.md)
