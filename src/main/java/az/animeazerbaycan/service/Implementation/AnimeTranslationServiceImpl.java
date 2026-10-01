package az.animeazerbaycan.service.Implementation;

import az.animeazerbaycan.entity.Anime;
import az.animeazerbaycan.entity.AnimeTranslated;
import az.animeazerbaycan.entity.Enum.TranslateStatus;
import az.animeazerbaycan.repository.AnimeRepository;
import az.animeazerbaycan.repository.AnimeTranslatedRepository;
import az.animeazerbaycan.service.Interface.AnimeTranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AnimeTranslationServiceImpl implements AnimeTranslationService {
    private static final Logger log = LoggerFactory.getLogger(AnimeTranslationServiceImpl.class);
    private final AnimeRepository animeRepository;
    private final AnimeTranslatedRepository animeTranslatedRepository;
    private final TransactionTemplate transactions;
    private final RestClient client;
    private final String apiKey;
    private final int batchSize;
    private final int maxChars;
    private final long requestIntervalMillis;
    private long lastRequestNanos;

    public AnimeTranslationServiceImpl(AnimeRepository animeRepository,
            AnimeTranslatedRepository animeTranslatedRepository,
            TransactionTemplate transactions,
            @Value("${app.anime-translation.libretranslate.base-url:http://127.0.0.1:5000}") String baseUrl,
            @Value("${app.anime-translation.libretranslate.api-key:}") String apiKey,
            @Value("${app.anime-translation.max-items:10}") int batchSize,
            @Value("${app.anime-translation.max-chars-per-request:2000}") int maxChars,
            @Value("${app.anime-translation.request-interval-ms:1000}") long requestIntervalMillis) {
        if (batchSize < 1 || maxChars < 1 || requestIntervalMillis < 0)
            throw new IllegalArgumentException(
                    "Translation batch size and character limit must be positive; interval cannot be negative");
        this.animeRepository = animeRepository;
        this.animeTranslatedRepository = animeTranslatedRepository;
        // Own short transactions; never hold a database transaction during an HTTP
        // request.
        this.transactions = new TransactionTemplate(transactions.getTransactionManager());
        this.apiKey = apiKey == null ? "" : apiKey;
        this.batchSize = batchSize;
        this.maxChars = maxChars;
        this.requestIntervalMillis = requestIntervalMillis;
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public void translateAnime() {
        translateAnime(batchSize);
    }

    @Override
    public synchronized int translateAnime(int limit) {
        if (limit < 1)
            throw new IllegalArgumentException("Translation limit must be positive");
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Call translation outside an existing database transaction");
        checkInterrupted();
        long afterId = 0;
        int examined = 0, saved = 0;
        while (examined < limit) {
            final long cursor = afterId;
            int pageSize = Math.min(50, limit - examined);
            List<Anime> candidates = transactions.execute(status -> animeRepository.findTranslationCandidates(
                    cursor, TranslateStatus.NOT_TRANSLATED, PageRequest.of(0, pageSize)));
            if (candidates == null || candidates.isEmpty())
                break;
            for (var original : candidates) {
                checkInterrupted();
                afterId = original.getId();
                examined++;
                String source = original.getSynopsis();
                if (source == null || source.isBlank())
                    continue;
                try {
                    String translated = translateSynopsis(source);
                    if (save(original.getId(), source, translated))
                        saved++;
                    log.info("Translation progress: examined {}, saved {}; last MAL ID {}", examined, saved,
                            original.getMalId());
                } catch (RuntimeException failure) {
                    // Pending status was never committed: interrupted/failed requests are safe to
                    // retry.
                    throw new IllegalStateException("Translation stopped at MAL ID " + original.getMalId()
                            + " after saving " + saved + " anime; rerun to continue pending records", failure);
                }
            }
        }
        log.info("Translation complete: examined {}, saved {}", examined, saved);
        return saved;
    }

    private String translateSynopsis(String source) {
        var lines = new ArrayList<String>();
        for (String line : source.split("\\R", -1)) {
            if (line.isBlank()) {
                lines.add(line);
                continue;
            }
            var translated = new ArrayList<String>();
            for (String part : chunks(line))
                translated.add(translateText(part));
            lines.add(String.join(" ", translated));
        }
        String result = String.join("\n", lines);
        if (result.isBlank())
            throw new IllegalStateException("LibreTranslate returned an empty synopsis");
        return result;
    }

    /**
     * Bound Unicode code points, preferring sentence boundaries and then
     * whitespace.
     */
    private List<String> chunks(String text) {
        var result = new ArrayList<String>();
        String remaining = text.strip();
        while (!remaining.isEmpty()) {
            int count = remaining.codePointCount(0, remaining.length());
            int end = remaining.offsetByCodePoints(0, Math.min(maxChars, count));
            if (count > maxChars) {
                int boundary = -1;
                for (int i = end - 1; i > 0; i--) {
                    if (Character.isWhitespace(remaining.charAt(i))
                            && ".!?".indexOf(remaining.charAt(i - 1)) >= 0) {
                        boundary = i;
                        break;
                    }
                }
                if (boundary < 1)
                    for (int i = end - 1; i > 0; i--)
                        if (Character.isWhitespace(remaining.charAt(i))) {
                            boundary = i;
                            break;
                        }
                if (boundary > 0)
                    end = boundary;
            }
            String part = remaining.substring(0, end).strip();
            if (!part.isEmpty())
                result.add(part);
            remaining = remaining.substring(end).stripLeading();
        }
        return result;
    }

    private String translateText(String text) {
        Map<String, String> request = new LinkedHashMap<>();
        request.put("q", text);
        request.put("source", "en");
        request.put("target", "az");
        request.put("format", "text");
        if (!apiKey.isBlank())
            request.put("api_key", apiKey);
        for (int attempt = 1;; attempt++) {
            checkInterrupted();
            throttle();
            try {
                var response = client.post().uri("/translate")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(request).retrieve().body(Map.class);
                Object value = response == null ? null : response.get("translatedText");
                if (!(value instanceof String translated) || translated.isBlank())
                    throw new IllegalStateException("Invalid LibreTranslate response: missing translatedText");
                return translated.strip();
            } catch (RestClientResponseException failure) {
                int code = failure.getStatusCode().value();
                if (attempt == 3 || (code != 429 && !failure.getStatusCode().is5xxServerError()))
                    // Do not include a server response body, which can echo input or credentials.
                    throw new IllegalStateException("LibreTranslate request failed with HTTP " + code);
                long wait = Math.max(2000L * attempt, retryAfterMillis(failure));
                log.warn("LibreTranslate HTTP {}; retrying after {} ms", code, wait);
                pause(wait);
            } catch (ResourceAccessException failure) {
                if (attempt == 3)
                    throw new IllegalStateException("LibreTranslate unavailable after three attempts");
                pause(2000L * attempt);
            }
        }
    }

    private boolean save(Long originalId, String source, String synopsis) {
        checkInterrupted();
        return Boolean.TRUE.equals(transactions.execute(status -> {
            var current = animeRepository.findFreshByIdForTranslation(originalId).orElse(null);
            if (current == null)
                return false;
            if (animeTranslatedRepository.findByMalId(current.getMalId()).isPresent()
                    || current.getTranslationStatus() != TranslateStatus.NOT_TRANSLATED
                    || !Objects.equals(current.getSynopsis(), source)) {
                log.info("Skipped MAL ID {}: original changed or translation already exists", current.getMalId());
                return false;
            }
            AnimeTranslated target = new AnimeTranslated();
            target.setMalId(current.getMalId());
            target.setTitle(current.getTitle());
            target.setSynopsis(synopsis);
            target.setPictureUrl(current.getPictureUrl());
            target.setStudio(current.getStudio());
            target.setStartDate(current.getStartDate());
            target.setEndDate(current.getEndDate());
            target.setMalMean(current.getMalMean());
            target.setMediaType(current.getMediaType());
            target.setAiringStatus(current.getAiringStatus());
            target.setNumEpisodes(current.getNumEpisodes());
            animeTranslatedRepository.saveAndFlush(target);
            current.setTranslationStatus(TranslateStatus.TRANSLATED);
            animeRepository.saveAndFlush(current);
            return true;
        }));
    }

    private void throttle() {
        if (lastRequestNanos != 0) {
            long elapsed = (System.nanoTime() - lastRequestNanos) / 1_000_000;
            if (elapsed < requestIntervalMillis)
                pause(requestIntervalMillis - elapsed);
        }
        lastRequestNanos = System.nanoTime();
    }

    private static long retryAfterMillis(RestClientResponseException failure) {
        String value = failure.getResponseHeaders() == null ? null
                : failure.getResponseHeaders().getFirst("Retry-After");
        if (value == null)
            return 0;
        try {
            return Math.min(60, Math.max(0, Long.parseLong(value))) * 1000;
        } catch (NumberFormatException ignored) {
            try {
                long millis = Duration.between(ZonedDateTime.now(),
                        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)).toMillis();
                return Math.min(60_000, Math.max(0, millis));
            } catch (java.time.DateTimeException invalid) {
                return 0;
            }
        }
    }

    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted())
            throw new IllegalStateException("Translation interrupted");
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Translation interrupted", failure);
        }
    }
}
