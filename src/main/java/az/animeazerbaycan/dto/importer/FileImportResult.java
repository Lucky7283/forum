package az.animeazerbaycan.dto.importer;

public record FileImportResult(int read, int imported, int skipped, int duplicates) {
}
