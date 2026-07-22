-- Clean existing data first (order matters for FK)
DELETE FROM cards;
DELETE FROM board_columns;
DELETE FROM boards;
DELETE FROM users WHERE username NOT IN ('admin', 'viewer', 'other-admin');

-- Pre-V3 seed data for migration integration tests
-- Users (use high IDs to avoid conflict with AdminBootstrap id=1)
INSERT INTO users (id, username, password_hash, enabled, created_at)
VALUES (10, 'mig-test-user-1', '$2a$10$dummy', TRUE, CURRENT_TIMESTAMP),
       (20, 'mig-test-user-2', '$2a$10$dummy', TRUE, CURRENT_TIMESTAMP);

-- Boards
INSERT INTO boards (id, name, author_id, created_at, updated_at)
VALUES (10, 'Board Test 1', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (20, 'Board Test 2', 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Columns
INSERT INTO board_columns (id, board_id, status, display_name, position)
VALUES (10, 10, 'TODO', 'To Do', 0),
       (20, 10, 'IN_PROGRESS', 'In Progress', 1),
       (30, 20, 'TODO', 'To Do', 0);

-- Cards (creator_id = board.author_id after backfill)
INSERT INTO cards (id, title, board_id, status, position, created_at, updated_at, creator_id, assignee_id)
VALUES (10, 'Card on Board 10', 10, 'TODO', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, NULL),
       (11, 'Second on Board 10', 10, 'IN_PROGRESS', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, NULL),
       (20, 'Card on Board 20', 20, 'TODO', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 20, NULL),
       (21, 'Second on Board 20', 20, 'TODO', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 20, NULL);
