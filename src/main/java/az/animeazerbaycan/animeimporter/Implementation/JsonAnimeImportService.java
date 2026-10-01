package az.animeazerbaycan.animeimporter.Implementation;

import az.animeazerbaycan.service.Interface.AnimeGenreService;

import az.animeazerbaycan.animeimporter.Interface.AnimeImportService;
import az.animeazerbaycan.dto.anime5k.Anime5kDto;
import az.animeazerbaycan.dto.importer.FileImportResult;
import az.animeazerbaycan.dto.importer.ImportResult;
import az.animeazerbaycan.dto.importer.PageImportResult;
import az.animeazerbaycan.mapper.Anime5kMapper;
import az.animeazerbaycan.entity.Anime;
import az.animeazerbaycan.repository.AnimeRepository;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.IntPredicate;

@Service
public class JsonAnimeImportService implements AnimeImportService {
    private static final Logger log = LoggerFactory.getLogger(JsonAnimeImportService.class);
    private final AnimeRepository originals;
    private final Anime5kMapper mapper;
    private final TransactionTemplate transactions;
    private final AnimeGenreService genres;
    private final String configuredFile;
    private final ObjectMapper json = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

    public JsonAnimeImportService(AnimeRepository originals, Anime5kMapper mapper,
                                  TransactionTemplate transactions, AnimeGenreService genres,
                                  @Value("${app.anime-import.file:classpath:anime_aggregated.jsonl}") String configuredFile) {
        this.originals = originals;
        this.mapper = mapper;
        this.transactions = transactions;
        this.genres = genres;
        this.configuredFile = configuredFile;
    }

    @Override
    public synchronized FileImportResult importFile(String location) {
        return read(location, id -> true).summary();
    }

    @Override
    public synchronized Long importAnime(int malId) {
        if (malId < 1) throw new IllegalArgumentException("MAL ID must be positive");
        var result = read(configuredFile, id -> id == malId);
        if (!result.saved().containsKey(malId))
            throw new IllegalStateException("No valid dataset entry for MAL ID " + malId);
        return result.saved().get(malId);
    }

    @Override
    public synchronized ImportResult importRange(int fromId, int toId) {
        if (fromId < 1 || toId < fromId || (long) toId - fromId >= 10000)
            throw new IllegalArgumentException("Specify a positive range of at most 10000 MAL IDs");
        var result = read(configuredFile, id -> id >= fromId && id <= toId);
        var skipped = new ArrayList<Integer>();
        for (long id = fromId; id <= toId; id++) if (!result.saved().containsKey((int) id)) skipped.add((int) id);
        return new ImportResult(result.summary().imported(), skipped);
    }

    @Override
    public PageImportResult importPages(int startPage) {
        throw new UnsupportedOperationException("JSON datasets have no API pages; use importFile(location)");
    }

    private Resource resource(String location) {
        if (location == null || location.isBlank()) throw new IllegalArgumentException("Set app.anime-import.file");
        if (location.startsWith("classpath:")) return new ClassPathResource(location.substring(10));
        if (location.startsWith("file:")) return new FileSystemResource(Path.of(java.net.URI.create(location)));
        return new FileSystemResource(Path.of(location));
    }

    private record Run(FileImportResult summary, Map<Integer, Long> saved) {
    }

    private Run read(String location, IntPredicate filter) {
        Resource resource = resource(location);
        if (!resource.exists() || !resource.isReadable())
            throw new IllegalArgumentException("Dataset is not readable: " + location);
        int read = 0, skipped = 0, duplicates = 0;
        var saved = new HashMap<Integer, Long>();
        try (var input = resource.getInputStream(); var parser = json.getFactory().createParser(input)) {
            var token = parser.nextToken();
            boolean array = token == JsonToken.START_ARRAY;
            if (array) token = parser.nextToken();
            while (token != null && token != JsonToken.END_ARRAY) {
                if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("Import interrupted");
                if (token != JsonToken.START_OBJECT)
                    throw new IllegalArgumentException("Expected anime object at record " + (read + 1));
                // Stream one object at a time; supports JSONL, arrays and single objects.
                var node = json.readTree(parser);
                read++;
                Anime5kDto dto;
                try {
                    dto = json.treeToValue(node, Anime5kDto.class);
                    mapper.validate(dto);
                } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException error) {
                    skipped++;
                    log.warn("Skipping dataset record {}: {}", read, error.getMessage());
                    token = parser.nextToken();
                    continue;
                }
                int id = dto.metadata().id();
                if (!filter.test(id)) {
                    skipped++;
                } else if (saved.containsKey(id)) {
                    duplicates++;
                    log.warn("Duplicate MAL ID {} at record {}; keeping first saved entry", id, read);
                } else {
                    Long localId = transactions.execute(status -> {
                        var original = originals.findByMalId(id).orElseGet(Anime::new);
                        mapper.apply(dto, original);
                        originals.saveAndFlush(original);
                        genres.replace(id, dto.metadata().genre());
                        return original.getId();
                    });
                    saved.put(id, localId);
                    if (saved.size() % 100 == 0)
                        log.info("JSON import progress: {} read, {} saved", read, saved.size());
                }
                token = parser.nextToken();
            }
            if (array && token != JsonToken.END_ARRAY) throw new IllegalArgumentException("Unclosed JSON array");
            if (array && parser.nextToken() != null)
                throw new IllegalArgumentException("Unexpected data after JSON array");
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("JSON import stopped near record " + Math.max(1, read)
                    + " after saving " + saved.size() + "; fix the file/error and rerun; committed rows remain saved", error);
        }
        var summary = new FileImportResult(read, saved.size(), skipped, duplicates);
        log.info("JSON import finished: {} read, {} saved, {} skipped, {} duplicates", read, saved.size(), skipped, duplicates);
        return new Run(summary, saved);
    }
}
