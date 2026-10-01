package az.animeazerbaycan.animeimporter.Implementation;

import az.animeazerbaycan.service.Interface.AnimeGenreService;

import az.animeazerbaycan.animeimporter.JikanClient;
import az.animeazerbaycan.dto.importer.ImportResult;
import az.animeazerbaycan.dto.importer.PageImportResult;
import az.animeazerbaycan.dto.jikan.JikanAnime;
import az.animeazerbaycan.dto.jikan.JikanReference;
import az.animeazerbaycan.dto.jikan.JikanLink;
import az.animeazerbaycan.dto.jikan.JikanPageResponse;
import az.animeazerbaycan.entity.*;
import az.animeazerbaycan.entity.Enum.TranslateStatus;
import az.animeazerbaycan.repository.*;
import az.animeazerbaycan.mapper.JikanMapper;
import lombok.AllArgsConstructor;
import az.animeazerbaycan.animeimporter.Interface.AnimeImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import java.util.*;

@Service
@AllArgsConstructor
public class JikanImportService implements AnimeImportService {
    private static final Logger log = LoggerFactory.getLogger(JikanImportService.class);
    private static final int MAX_ATTEMPTS = 3;

    private final JikanClient client;
    private final AnimeRepository originals;
    private final AnimeTranslatedRepository translated;
    private final AnimeGenreService genres;
    private final JikanMapper mapper;
    private final TransactionTemplate transactions;

    /**
     * Each anime commits independently, so a failed run can resume at its failed
     * ID.
     */
    @Override
    public synchronized ImportResult importRange(int fromId, int toId) {
        if (fromId < 1 || toId < fromId)
            throw new IllegalArgumentException("Specify a positive IDs");
        int imported = 0;
        List<Integer> skipped = new ArrayList<>();
        for (int id = fromId;; id++) {
            checkInterrupted();
            try {
                var data = fetchForBatch(id);
                if (data == null) {
                    skipped.add(id);
                    log.warn("Jikan returned 404 for MAL ID {}; skipped", id);
                } else {
                    saveAnime(id, data);
                    imported++;
                    log.info("Imported MAL ID {}; saved {}, skipped {} in this run", id, imported, skipped.size());
                }
            } catch (RuntimeException failure) {
                throw new IllegalStateException("Import error at MAL ID " + id
                        + " after saving " + imported + " anime; resume with app.jikan.import.from-id=" + id, failure);
            }
            if (id == toId)
                break;
            pause(1100);
        }
        log.info("Jikan range import complete: saved {}, skipped IDs {}", imported, skipped);
        return new ImportResult(imported, skipped);
    }

    /**
     * Imports original records only. Each record commits separately; rerunning a
     * page is safe.
     */
    @Override
    public synchronized PageImportResult importPages(int startPage) {
        if (startPage < 1)
            throw new IllegalArgumentException("Start page must be positive");
        int imported = 0;
        for (int page = startPage;; page++) {
            checkInterrupted();
            try {
                var response = fetchPageWithRetry(page);
                for (var item : response.data()) {
                    checkInterrupted();
                    // List entries identify records; only /full responses are persisted.
                    pause(1100);
                    var data = fetchForBatch(item.malId());
                    if (data == null) {
                        log.warn("Skipping MAL ID {} on page {}: full resource returned 404", item.malId(), page);
                        continue;
                    }
                    transactions.executeWithoutResult(status -> {
                        var original = originals.findByMalId(data.malId()).orElseGet(Anime::new);
                        mapper.apply(data, original);
                        originals.saveAndFlush(original);
                        genres.replace(data.malId(), data.genres() == null ? null
                                : data.genres().stream().map(JikanReference::name).toList());
                    });
                    imported++;
                }
                log.info("Imported Jikan page {}; saved {} originals in this run", page, imported);
                if (!response.pagination().hasNextPage())
                    return new PageImportResult(imported, page);
            } catch (RuntimeException failure) {
                throw new IllegalStateException("Import stopped after saving " + imported
                        + " originals; resume with app.anime-import.start-page=" + page, failure);
            }
            pause(1100);
        }
    }

    private JikanPageResponse fetchPageWithRetry(int page) {
        for (int attempt = 1;; attempt++) {
            checkInterrupted();
            try {
                log.info("Requesting Jikan page {}, attempt {}/{}", page, attempt, MAX_ATTEMPTS);
                return client.fetchPage(page);
            } catch (RestClientResponseException failure) {
                if (attempt == MAX_ATTEMPTS || (failure.getStatusCode().value() != 429
                        && !failure.getStatusCode().is5xxServerError()))
                    throw failure;
                long delay = Math.max(2000L * attempt, retryAfterMillis(failure));
                log.warn("Jikan page {} returned HTTP {} on attempt {}/{}; retrying in {} ms",
                        page, failure.getStatusCode().value(), attempt, MAX_ATTEMPTS, delay, failure);
                pause(delay);
            } catch (ResourceAccessException failure) {
                if (attempt == MAX_ATTEMPTS)
                    throw failure;
                log.warn("Network failure fetching Jikan page {}, attempt {}/{}; retrying in {} ms",
                        page, attempt, MAX_ATTEMPTS, 2000L * attempt, failure);
                pause(2000L * attempt);
            }
        }
    }

    private JikanAnime fetchForBatch(int id) {
        for (int attempt = 1;; attempt++) {
            checkInterrupted();
            try {
                return client.fetch(id);
            } catch (RestClientResponseException failure) {
                int status = failure.getStatusCode().value();
                if (status == 404)
                    return null;
                if (attempt == MAX_ATTEMPTS || (status != 429 && !failure.getStatusCode().is5xxServerError()))
                    throw failure;
                long delay = Math.max(2000L * attempt, retryAfterMillis(failure));
                log.warn("Jikan request failed for MAL ID {}; retrying attempt {}", id, attempt + 1);
                pause(delay);
            } catch (ResourceAccessException failure) {
                if (attempt == MAX_ATTEMPTS)
                    throw failure;
                pause(2000L * attempt);
            }
        }
    }

    private static long retryAfterMillis(RestClientResponseException failure) {
        String value = failure.getResponseHeaders() == null ? null
                : failure.getResponseHeaders().getFirst("Retry-After");
        if (value == null)
            return 0;
        try {
            return Duration.ofSeconds(Math.max(0, Long.parseLong(value))).toMillis();
        } catch (NumberFormatException ignored) {
            try {
                return Math.max(0, Duration.between(ZonedDateTime.now(),
                        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)).toMillis());
            } catch (java.time.DateTimeException invalid) {
                return 0;
            }
        }
    }

    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted())
            throw new IllegalStateException("Import interrupted");
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Import interrupted", failure);
        }
    }

    // Single-anime import remains available.
    @Override
    public Long importAnime(int malId) {
        return saveAnime(malId, client.fetch(malId));
    }

    private Long saveAnime(int malId, JikanAnime data) {
        return transactions.execute(status -> {
            var original = originals.findByMalId(malId).orElseGet(Anime::new);
            mapper.apply(data, original);
            originals.saveAndFlush(original);
            genres.replace(malId, data.genres() == null ? null
                    : data.genres().stream().map(JikanReference::name).toList());
            // Existing translated text is preserved; external links still belong to its card.
            translated.findByMalId(malId).ifPresent(local -> {
                original.setTranslationStatus(TranslateStatus.TRANSLATED);
                List<JikanLink> links = new ArrayList<>();
                if (data.external() != null)
                    links.addAll(data.external());
                if (data.streaming() != null)
                    links.addAll(data.streaming());
                Set<String> existing = new HashSet<>();
                local.getExternalLinks().forEach(l -> existing.add(l.getUrl()));
                for (var source : links)
                    if (source.name() != null && source.url() != null && source.url().matches("https?://[^\\s]+")
                            && existing.add(source.url())) {
                        ExternalLink l = new ExternalLink();
                        l.setAnime(local);
                        l.setLabel(source.name());
                        l.setUrl(source.url());
                        local.getExternalLinks().add(l);
                    }
            });
            return original.getId();
        });
    }

}
