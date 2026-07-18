# Модели данных — Backend

**БД:** PostgreSQL 16 (runtime), H2 in-memory (профиль `test`)  
**Миграции:** Flyway (`src/main/resources/db/migration/`)  
**ORM:** Spring Data JPA, `ddl-auto: validate`

## Таблицы

### users
| Поле | Тип | Ограничения |
|------|-----|-------------|
| id | BIGINT | PK, identity |
| username | VARCHAR(100) | UNIQUE, NOT NULL |
| password_hash | VARCHAR(255) | NOT NULL (BCrypt) |
| enabled | BOOLEAN | DEFAULT TRUE |
| created_at | TIMESTAMP | DEFAULT now |

### roles
| Поле | Тип | Ограничения |
|------|-----|-------------|
| id | BIGINT | PK |
| name | VARCHAR(50) | UNIQUE — сейчас используется `ADMIN` |

### user_roles
Many-to-many: `(user_id, role_id)` PK, FK → users, roles.

### boards
| Поле | Тип | Ограничения |
|------|-----|-------------|
| id | BIGINT | PK |
| name | VARCHAR(120) | NOT NULL |
| author_id | BIGINT | FK → users |
| created_at / updated_at | TIMESTAMP | |
| | | UNIQUE (author_id, name) |

### board_columns
| Поле | Тип | Ограничения |
|------|-----|-------------|
| id | BIGINT | PK |
| board_id | BIGINT | FK → boards ON DELETE CASCADE |
| status | VARCHAR(30) | CHECK IN (`TODO`, `IN_PROGRESS`, `DONE`), UNIQUE per board |
| display_name | VARCHAR(80) | UI-название колонки |
| position | INTEGER | CHECK 0..2 |

Новая доска всегда получает ровно три системных статуса; их нельзя добавить/удалить/заменить — только переименовать и переставить.

### cards
| Поле | Тип | Ограничения |
|------|-----|-------------|
| id | BIGINT | PK |
| board_id | BIGINT | FK → boards ON DELETE CASCADE |
| title | VARCHAR(200) | NOT NULL |
| description | VARCHAR(4000) | nullable |
| status | VARCHAR(30) | CHECK same enum |
| position | INTEGER | >= 0, нормализуется после мутаций |
| created_at / updated_at | TIMESTAMP | |

## Индексы

- `idx_user_roles_role_id`
- `idx_boards_author_id`
- `idx_board_columns_board_position`
- `idx_cards_board_status_position`

## Миграции

| Файл | Содержание |
|------|------------|
| `V1__create_users_and_roles.sql` | users, roles, user_roles |
| `V2__create_kanban_schema.sql` | boards, board_columns, cards |

## JPA-сущности

- `user/User.java`, `user/Role.java`
- `board/Board.java`, `board/BoardColumn.java`, `board/ColumnStatus.java`
- `card/Card.java`

## Bootstrap

`config/AdminBootstrap.java` создаёт роль `ADMIN` и пользователя из `app.admin.username` / `app.admin.password` при старте, если их ещё нет.
