ALTER TABLE cards ADD COLUMN creator_id BIGINT;
ALTER TABLE cards ADD COLUMN assignee_id BIGINT;

UPDATE cards c
SET creator_id = b.author_id
FROM boards b
WHERE c.board_id = b.id;

ALTER TABLE cards ALTER COLUMN creator_id SET NOT NULL;
ALTER TABLE cards ADD CONSTRAINT fk_cards_creator FOREIGN KEY (creator_id) REFERENCES users(id);
ALTER TABLE cards ADD CONSTRAINT fk_cards_assignee FOREIGN KEY (assignee_id) REFERENCES users(id);

CREATE INDEX idx_cards_creator_id ON cards(creator_id);
CREATE INDEX idx_cards_assignee_id ON cards(assignee_id);
