package az.animeazerbaycan.dto.importer;

import java.util.List;

public record ImportResult(int imported, List<Integer> skippedIds) {
    public ImportResult {
        skippedIds = List.copyOf(skippedIds);
    }
}
