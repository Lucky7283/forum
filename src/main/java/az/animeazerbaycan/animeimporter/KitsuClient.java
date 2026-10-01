package az.animeazerbaycan.animeimporter;

import az.animeazerbaycan.dto.kitsu.KitsuAnime;
import az.animeazerbaycan.dto.kitsu.KitsuGenrePage;

import java.util.List;
import java.util.LinkedHashSet;

import az.animeazerbaycan.dto.kitsu.KitsuLinks;
import az.animeazerbaycan.dto.kitsu.KitsuMappingResponse;
import az.animeazerbaycan.dto.kitsu.KitsuPageResponse;
import az.animeazerbaycan.dto.kitsu.KitsuResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

@Component
public class KitsuClient {
    // Matches the attached ten-entry response and keeps start-page resumable.
    public static final int PAGE_SIZE = 10;
    private static final int MAPPING_PAGE_SIZE = 20;
    private static final String MAL_SITE = "myanimelist/anime";
    private final RestClient client;

    public KitsuClient(@Value("${app.kitsu.base-url:https://kitsu.app/api/edge}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
                .defaultHeader("Accept", "application/vnd.api+json").build();
    }

    public KitsuPageResponse fetchPage(int page) {
        if (page < 1)
            throw new IllegalArgumentException("Page must be positive");
        var result = get(uri -> uri.path("/anime").queryParam("sort", "id")
                        .queryParam("page[limit]", PAGE_SIZE).queryParam("page[offset]", ((long) page - 1) * PAGE_SIZE).build(),
                KitsuPageResponse.class);
        if (result == null || result.data() == null || (result.data().isEmpty() && hasNext(result)))
            throw new IllegalStateException("Invalid Kitsu page response");
        result.data().forEach(KitsuClient::validateAnime);
        return result;
    }

    public List<String> fetchGenreNames(String kitsuId) {
        positiveId(kitsuId);
        var names = new LinkedHashSet<String>();
        for (long offset = 0; ; offset = Math.addExact(offset, MAPPING_PAGE_SIZE)) {
            long currentOffset = offset;
            var page = get(uri -> uri.path("/anime/{id}/genres")
                            .queryParam("page[limit]", MAPPING_PAGE_SIZE)
                            .queryParam("page[offset]", currentOffset).build(kitsuId),
                    KitsuGenrePage.class);
            if (page == null || page.data() == null)
                throw new IllegalStateException("Invalid Kitsu genres response");
            boolean more = page.links() == null ? page.data().size() == MAPPING_PAGE_SIZE : next(page.links());
            if (page.data().isEmpty() && more)
                throw new IllegalStateException("Empty Kitsu genres page claims another page");
            for (var genre : page.data()) {
                if (genre == null || !"genres".equals(genre.type()) || genre.attributes() == null
                        || genre.attributes().name() == null || genre.attributes().name().isBlank())
                    throw new IllegalStateException("Invalid Kitsu genre");
                names.add(genre.attributes().name());
            }
            if (!more) return List.copyOf(names);
            pause(1100);
        }
    }

    public boolean hasNext(KitsuPageResponse page) {
        // The supplied sample omits links. In that case a full page requires one more
        // request.
        return page.links() == null ? page.data().size() == PAGE_SIZE : next(page.links());
    }

    public Integer findMalId(String kitsuId) {
        positiveId(kitsuId);
        Integer malId = null;
        for (long offset = 0; ; offset += MAPPING_PAGE_SIZE) {
            long currentOffset = offset;
            var result = mappings(uri -> uri.path("/anime/{id}/mappings")
                    .queryParam("page[limit]", MAPPING_PAGE_SIZE).queryParam("page[offset]", currentOffset)
                    .build(kitsuId));
            for (var mapping : result.data()) {
                var attributes = mapping.attributes();
                if (!MAL_SITE.equals(attributes.externalSite()))
                    continue;
                int found = positiveId(attributes.externalId());
                if (malId != null && malId != found)
                    throw new IllegalStateException("Conflicting MAL mappings for Kitsu ID " + kitsuId);
                malId = found;
            }
            if (!moreMappings(result))
                return malId;
            pause(1100);
        }
    }

    /**
     * Interface IDs are MAL IDs, never Kitsu IDs. Null means no matching mapping.
     */
    public KitsuAnime fetchByMalId(int malId) {
        if (malId < 1)
            throw new IllegalArgumentException("MAL ID must be positive");
        String kitsuId = null;
        for (long offset = 0; ; offset += MAPPING_PAGE_SIZE) {
            long currentOffset = offset;
            var result = mappings(uri -> uri.path("/mappings").queryParam("include", "item")
                    .queryParam("filter[externalSite]", MAL_SITE)
                    .queryParam("filter[externalId]", malId).queryParam("page[limit]", MAPPING_PAGE_SIZE)
                    .queryParam("page[offset]", currentOffset).build());
            for (var mapping : result.data()) {
                if (!MAL_SITE.equals(mapping.attributes().externalSite())
                        || positiveId(mapping.attributes().externalId()) != malId)
                    throw new IllegalStateException("Unexpected Kitsu MAL mapping");
                var item = mapping.relationships() == null ? null : mapping.relationships().get("item");
                if (item == null || item.data() == null || !"anime".equals(item.data().type()))
                    throw new IllegalStateException("Missing Kitsu anime mapping relationship");
                positiveId(item.data().id());
                if (kitsuId != null && !kitsuId.equals(item.data().id()))
                    throw new IllegalStateException("Conflicting Kitsu mappings for MAL ID " + malId);
                kitsuId = item.data().id();
            }
            if (!moreMappings(result))
                break;
            pause(1100);
        }
        if (kitsuId == null)
            return null;
        String id = kitsuId;
        pause(1100);
        var response = get(uri -> uri.path("/anime/{id}").build(id), KitsuResponse.class);
        if (response == null)
            throw new IllegalStateException("Empty Kitsu response");
        validateAnime(response.data());
        if (!id.equals(response.data().id()))
            throw new IllegalStateException("Kitsu response ID mismatch");
        return response.data();
    }

    private KitsuMappingResponse mappings(Function<UriBuilder, URI> uri) {
        var result = get(uri, KitsuMappingResponse.class);
        if (result == null || result.data() == null || (result.data().isEmpty() && next(result.links())))
            throw new IllegalStateException("Invalid Kitsu mappings response");
        for (var mapping : result.data())
            if (mapping == null || !"mappings".equals(mapping.type()) || mapping.attributes() == null)
                throw new IllegalStateException("Invalid Kitsu mapping");
        return result;
    }

    private static boolean moreMappings(KitsuMappingResponse response) {
        return response.links() == null ? response.data().size() == MAPPING_PAGE_SIZE : next(response.links());
    }

    private static boolean next(KitsuLinks links) {
        return links != null && links.next() != null && !links.next().isBlank();
    }

    private static void validateAnime(KitsuAnime anime) {
        if (anime == null || !"anime".equals(anime.type()) || anime.attributes() == null)
            throw new IllegalStateException("Invalid Kitsu anime resource");
        positiveId(anime.id());
    }

    private static int positiveId(String value) {
        try {
            int id = Integer.parseInt(value);
            if (id > 0)
                return id;
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalStateException("Invalid Kitsu identity: " + value);
    }

    private <T> T get(Function<UriBuilder, URI> uri, Class<T> type) {
        for (int attempt = 1; ; attempt++) {
            checkInterrupted();
            try {
                return client.get().uri(uri).retrieve().body(type);
            } catch (RestClientResponseException failure) {
                if (attempt == 3
                        || (failure.getStatusCode().value() != 429 && !failure.getStatusCode().is5xxServerError()))
                    throw failure;
                pause(Math.max(2000L * attempt, retryAfterMillis(failure)));
            } catch (ResourceAccessException failure) {
                if (attempt == 3)
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
            return Math.multiplyExact(Math.max(0, Long.parseLong(value)), 1000L);
        } catch (NumberFormatException | ArithmeticException ignored) {
            try {
                return Math.max(0, Duration.between(ZonedDateTime.now(),
                        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)).toMillis());
            } catch (java.time.DateTimeException invalid) {
                return 0;
            }
        }
    }

    public static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted())
            throw new IllegalStateException("Kitsu import interrupted");
    }

    public static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kitsu import interrupted", failure);
        }
    }
}
