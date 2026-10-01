-- Run against PostgreSQL with the application stopped, before deploying reply support.
-- Existing comments remain top-level; no rows or text are removed.
BEGIN;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS parent_id bigint;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                   WHERE conrelid = 'comments'::regclass AND conname = 'fk_comments_parent') THEN
        ALTER TABLE comments ADD CONSTRAINT fk_comments_parent
            FOREIGN KEY (parent_id) REFERENCES comments(id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_comments_parent_id ON comments(parent_id);
COMMIT;
