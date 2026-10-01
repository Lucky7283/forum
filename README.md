# Anime Azerbaijan backend

Java 21, Spring Boot 4.0.5, PostgreSQL, Flyway. Backend only, following the supplied v0.5 plan.

## Configuration and run

Set the environment variables listed in `.env.example` before starting. Spring does not automatically load `.env`. Use an existing PostgreSQL database. `JWT_SIGNING_KEY` must contain at least 32 UTF-8 bytes. `JWT_TTL` is an ISO-8601 duration, such as `PT24H`.

Run `mvn spring-boot:run`. Run checks with `mvn test`. A full Java 21 JDK is required (a runtime alone is insufficient). This workspace has a local JDK in `.tooling/jdk-21.0.12.1+1`; to use it here, run `JAVA_HOME="$PWD/.tooling/jdk-21.0.12.1+1" mvn -Dmaven.repo.local="$PWD/.maven-cache" test`. Swagger UI is at `/swagger-ui/index.html`.

Authentication uses a Secure, HttpOnly, SameSite=Strict cookie. Serve the frontend and API over HTTPS on the same site. Mutating requests must include `Origin` matching `CLIENT_ORIGIN`; browser requests must include credentials. Logout clears the cookie.

## Contract details

Endpoints follow sections 5–7 of the plan. Page numbers start at 0; size defaults to 20 and must be 1–100. Catalog sorts: `title`, `malMean`, `createdAt`, `startDate`, `popularity`, with `asc` or `desc`. Popularity is the number of local ratings. Genre filtering uses one genre slug. Search uses the translated title. Community anime IDs refer to `anime_translated.id`; the detail slug is stored in the original `anime` row.

Ratings currently accept `{"localScore":9}`: the single local 1–10 score in the domain model. MAL scores are imported as `malMean` and are never overwritten by user ratings. The document's two-score request example conflicts with that model.

Username: 3–50 letters, digits, or underscores. Password: at least 8 characters, at most 72 UTF-8 bytes. Profile PATCH updates only supplied fields; `avatarUrl: null` clears the optional avatar. Public profile responses exclude email and password.

## Jikan import

`JikanImportService.importAnime(malId)` is an explicitly invoked service. There is no import endpoint, startup import, or scheduler. It fetches `/anime/{id}/full` once and upserts the original anime in a transaction. Re-imports preserve the slug and translated text. Catalog/detail reads use PostgreSQL and do not call Jikan.

Genre reference data is imported with the original. Genre associations and external/streaming links are attached when an Azerbaijani translated row exists for that MAL ID. Re-import after inserting a translated row to attach those relations. Existing local links are preserved. The backend does not invent or automatically translate Azerbaijani text.


## Paginated original anime import

Enable explicitly with application arguments:

```sh
mvn spring-boot:run -Dspring-boot.run.arguments="--app.anime-import.provider=jikan --app.anime-import.start-page=1"
```

The runner requests `/anime?page=N&limit=25&order_by=mal_id&sort=asc`, decodes
`data` and `pagination` into DTOs, and maps each entry into the original `anime`
table. It stops when `has_next_page=false`. Existing originals are updated by
MAL ID with their slug preserved. Translations, genres and links are untouched
by this mode. Only fields supported by the existing Anime entity are stored;
this is not a raw JSON archive. List responses do not include all `/full` fields.

Each original commits separately, with no database transaction held during HTTP
requests. Pages are requested sequentially with a 1.1 second gap. Network errors,
429 and 5xx are retried up to three attempts with backoff and Retry-After support.
An exhausted page or database error stops the import and logs the page to resume;
rerunning that page updates already saved entries without duplicating MAL IDs.
The server remains running. Import is disabled by default. Existing rows do not
prevent an explicitly enabled import. Old from-id/to-id flags do not control this
runner. Pagination is not a snapshot: catalog changes during a run can shift pages.


## Import provider selection

Set `app.anime-import.provider` to `none` (default), `jikan`, or `anilist`.
Environment equivalents: `ANIME_IMPORT_PROVIDER` and `ANIME_IMPORT_START_PAGE`.
Unknown providers fail configuration explicitly. The old Jikan enabled/start-page
properties no longer select or start the runner. The Jikan service itself is unchanged.

For a limited AniList batch, use program arguments:

```
--app.anime-import.provider=anilist --app.anime-import.start-page=1
```

AniList uses its GraphQL endpoint, separate DTOs and a separate implementation of
`AnimeImportService`. It requests pages of 25 entries ordered by AniList ID and
examines at most 100 entries per run. Entries without a MAL ID are skipped, so the
number saved can be smaller. Existing originals are matched by MAL ID, retaining
slugs, translation status and MAL scores. Translated records are untouched.
AniList ratings are deliberately not stored as MAL ratings. Partial dates remain
null; cancelled titles map to finished_airing and hiatus to currently_airing under
the existing three-status model. Original metadata can be refreshed from AniList.

`importAnime` and `importRange` accept MAL IDs, not AniList IDs. Ranges are limited
to 100 IDs; the legacy `importFirst500Anime` operation explicitly rejects the
request for this provider. Each save has its own transaction. Network/429/5xx
errors get up to three attempts, with 30/60-second backoff (or a longer numeric
Retry-After). GraphQL errors stop the import and are logged; the server stays up.
The 100-entry cap is per invocation, not a lifetime database limit or provider
permission. Review AniList usage terms for your project:
https://docs.anilist.co/guide/terms-of-use

Both provider DTO containers live in `az.animeazerbaycan.dto`. After moving DTO
classes, clean-rebuild the project to remove stale compiled classes before launch.
These changes have not been run against AniList or your database.


## Jikan full-resource import

The page importer now uses `GET /anime` for page one and `GET /anime?page=N`
for subsequent pages, with the endpoint's default page size/order. Each listed
MAL ID is fetched through `GET /anime/{id}/full` before saving. No request body
is sent. `JikanDtos.Anime` represents the full resource, including alternative
titles, trailer, studios, genres, relations, opening/ending themes and links.
Only fields represented by the existing Anime entity are persisted in the
original table; other DTO fields are not a database archive. No schema changes.
The page importer does not modify translations or their links/genres.

List requests and full requests are separated by 1.1-second pauses; retries
retain their backoff. This makes full imports slower than list-only imports.
404 full resources are logged/skipped; other exhausted errors stop with a resume
page. Use `app.anime-import.start-page`, not the old Jikan-specific page property.
This update has not been executed or tested automatically at the user's request.


## Anime5K JSON / JSONL file import

Place your real dataset at `src/main/resources/anime_aggregated.jsonl` alongside
application.properties. Select `app.anime-import.provider=json`. The setting
`app.anime-import.file=${ANIME_IMPORT_FILE:classpath:anime_aggregated.jsonl}`
controls the file; `classpath:` works inside a packaged JAR as well as in the IDE.
Rebuild a packaged application after replacing a bundled resource. For changing
files without rebuilding, set an external absolute path or `file:/...` instead.

Program arguments:
```
--app.anime-import.provider=json
```
Or use a different dataset location with the same Anime5K schema:
```
--app.anime-import.provider=json --app.anime-import.file=/absolute/path/anime.jsonl
```

JsonAnimeImportService implements AnimeImportService.importFile(String), using
Anime5kDto and Anime5kMapper. Supports a single object, JSON array, or JSONL;
CSV/ZIP and other schemas are not supported. A replacement with a different
schema needs a matching DTO/mapper, not just a new filename. No API requests.

The file is streamed and has no 100-record limit. ID is treated as MAL ID based
on the Anime5K source. Title, type, studio, score and synopsis map to original
Anime records. Existing records update by MAL ID, preserving slugs, translation
status and absent fields. Full YYYY-MM-DD dates are stored; partial YYYY-MM or
YYYY dates do not invent missing days and preserve existing precise dates.
New records retain null for fields absent in the dataset. Genres, characters,
voice actors, rank, popularity and related titles are read into DTOs but are not
persisted because the original Anime entity has no fields for them. Translated
records and their relationships are untouched. The dataset does not generate
daily dashboard recommendations; that feature is not currently implemented.

Duplicates in a file are skipped after the first successful save. Invalid field
values are logged/skipped. Malformed JSON, unreadable files and database errors
stop the import with progress; committed records remain and reruns update them.
The report shows read, saved (inserts plus updates), skipped and duplicate counts.
An empty file reports zero. One transaction per save. Saved IDs are retained for
deduplication; the full dataset is not loaded into memory.

The single/range methods filter the configured file by MAL ID. First500 means
IDs 1..500, not the first 500 rows. File mode rejects importPages and is dispatched
directly to importFile. No real dataset is bundled by this change.
Application, database import and tests have not been run (manual verification).
