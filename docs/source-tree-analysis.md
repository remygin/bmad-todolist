# Анализ дерева исходников

Дата: 2026-07-22  
Репозиторий: multi-part (frontend + backend + autotests)

```
bmad-todolist/
├── README.md                 # Обзор, быстрый старт, unit/IT и E2E
├── .env.example              # Шаблон переменных окружения
├── docker-compose.yml        # db + backend + frontend
├── docs/                     # Сгенерированная AI-документация (этот каталог)
│
├── backend/                  # Part: backend (Spring Boot API)
│   ├── Dockerfile            # multi-stage: Maven build → JRE 17
│   ├── pom.xml               # Spring Boot 4.0.7, Java 17
│   ├── mvnw / .mvn/
│   └── src/
│       ├── main/
│       │   ├── java/com/bmad/todolist/
│       │   │   ├── TodolistApplication.java   # ENTRY
│       │   │   ├── auth/       # JWT login, filter, UserDetails
│       │   │   ├── board/      # доски и колонки (REST + JPA)
│       │   │   ├── card/       # карточки (REST + JPA)
│       │   │   ├── user/       # User/Role entities
│       │   │   ├── config/     # Security, AdminBootstrap
│       │   │   └── common/     # exceptions, ErrorResponse
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/   # Flyway V1, V2
│       └── test/java/.../      # Unit/IT: Auth + Kanban (MockMvc, H2)
│
├── frontend/                 # Part: frontend (React SPA)
│   ├── Dockerfile            # Node build → nginx:1.27
│   ├── nginx.conf            # SPA + proxy /api → backend
│   ├── package.json
│   ├── vite.config.ts        # dev proxy → localhost:8081
│   └── src/
│       ├── main.tsx          # ENTRY
│       ├── App.tsx           # маршруты
│       ├── api/              # HTTP-клиент → /api
│       ├── auth/             # AuthContext, ProtectedRoute
│       ├── pages/            # Login, AdminShell, Boards
│       └── components/kanban/# DnD доска/колонки/карточки
│
└── autotests/                # Part: autotests (E2E API + UI)
    ├── pom.xml               # Java 17, JUnit 5, Rest Assured, Selenide
    ├── mvnw / .mvn/
    └── src/test/
        ├── java/.../autotests/
        │   ├── api/          # Rest Assured client
        │   ├── config/       # properties + stack reachability
        │   ├── ui/           # Selenide setup + Page Objects
        │   └── tests/        # api/ + ui/ smoke scenarios
        └── resources/
            └── autotest.properties
```

## Критические каталоги

| Путь | Назначение |
|------|------------|
| `backend/.../auth` | JWT-аутентификация |
| `backend/.../board`, `card` | Kanban domain + REST |
| `backend/.../db/migration` | Схема БД |
| `backend/src/test` | Unit / интеграционные тесты кода |
| `frontend/src/api` | Контракт с backend |
| `frontend/src/components/kanban` | UI drag-and-drop |
| `frontend/src/auth` | Сессия (sessionStorage JWT) |
| `autotests/src/test` | E2E против живого стенда (не MockMvc) |

## Точки входа

- Backend: `TodolistApplication.java`, порт `SERVER_PORT` (default 8080; локально часто 8081)
- Frontend: `main.tsx` → Vite :5173; Docker: nginx :80
- Autotests: `mvn test` в `autotests/` (нужен поднятый стенд)
