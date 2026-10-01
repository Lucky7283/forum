-- Run once against the EXISTING PostgreSQL database, with the application stopped,
-- before starting the updated code. The old anime_genre table is retained.
-- This is manual: Flyway remains disabled. Review/back up the database first.
BEGIN;
SELECT pg_advisory_xact_lock(hashtext('forum:genre-links-by-mal-id:v1'));
CREATE TABLE IF NOT EXISTS app_manual_migrations (
    name varchar(160) PRIMARY KEY,
    applied_at timestamptz NOT NULL DEFAULT now()
);
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM app_manual_migrations WHERE name = 'genre-links-by-mal-id-v1') THEN
        RAISE NOTICE 'Genre link migration already applied';
        RETURN;
    END IF;

    -- Other sources supply genre names, not MAL genre IDs. Do not invent IDs.
    ALTER TABLE genre ALTER COLUMN mal_id DROP NOT NULL;
    CREATE TABLE IF NOT EXISTS anime_genre_mal (
        anime_mal_id integer NOT NULL,
        genre_id bigint NOT NULL,
        PRIMARY KEY (anime_mal_id, genre_id)
    );
    -- Add constraints even if Hibernate has already created the new table.
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'anime_genre_mal'::regclass
                   AND conname = 'fk_anime_genre_mal_anime') THEN
        ALTER TABLE anime_genre_mal ADD CONSTRAINT fk_anime_genre_mal_anime
            FOREIGN KEY (anime_mal_id) REFERENCES anime(mal_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'anime_genre_mal'::regclass
                   AND contype = 'f' AND confrelid = 'genre'::regclass) THEN
        ALTER TABLE anime_genre_mal ADD CONSTRAINT fk_anime_genre_mal_genre
            FOREIGN KEY (genre_id) REFERENCES genre(id);
    END IF;
    CREATE INDEX IF NOT EXISTS idx_anime_genre_mal_genre ON anime_genre_mal(genre_id, anime_mal_id);

    IF to_regclass('anime_genre') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1 FROM anime_genre old_link
            LEFT JOIN anime_translated t ON t.id = old_link.anime_id
            LEFT JOIN anime a ON a.mal_id = t.mal_id
            LEFT JOIN genre g ON g.id = old_link.genre_id
            WHERE t.id IS NULL OR a.id IS NULL OR g.id IS NULL
        ) THEN
            RAISE EXCEPTION 'Unresolvable legacy genre links: fix orphan rows before migrating; nothing committed';
        END IF;
        INSERT INTO anime_genre_mal(anime_mal_id, genre_id)
        SELECT t.mal_id, old_link.genre_id
        FROM anime_genre old_link JOIN anime_translated t ON t.id = old_link.anime_id
        ON CONFLICT (anime_mal_id, genre_id) DO NOTHING;
    END IF;
    INSERT INTO app_manual_migrations(name) VALUES ('genre-links-by-mal-id-v1');
END $$;
COMMIT;
