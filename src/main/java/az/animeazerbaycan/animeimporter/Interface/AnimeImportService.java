package az.animeazerbaycan.animeimporter.Interface;

import az.animeazerbaycan.dto.importer.FileImportResult;
import az.animeazerbaycan.dto.importer.ImportResult;
import az.animeazerbaycan.dto.importer.PageImportResult;

/** Import operations shared by providers. All ID arguments refer to MyAnimeList IDs. */
public interface AnimeImportService {
    /** Imports one anime and returns its local database ID. */
    Long importAnime(int malId);

    /** Imports an inclusive range of MAL IDs, reporting unavailable IDs. */
    ImportResult importRange(int fromId, int toId);

    /** Imports originals from a one-based provider page, subject to the provider batch limit. */
    PageImportResult importPages(int startPage);

    /** Local file path or classpath: resource. API-only implementations reject this operation. */
    default FileImportResult importFile(String location) {
        throw new UnsupportedOperationException("This provider does not import files");
    }
}
