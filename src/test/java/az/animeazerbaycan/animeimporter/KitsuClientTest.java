package az.animeazerbaycan.animeimporter;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class KitsuClientTest {
    @Test void deserializesAttachedSampleAndUsesStableOffsetPagination() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var query = new AtomicReference<String>();
        var accept = new AtomicReference<String>();
        byte[] sample;
        try (var stream = getClass().getResourceAsStream("/kitsu/anime-page.json")) { sample = stream.readAllBytes(); }
        server.createContext("/api/edge/anime", exchange -> {
            query.set(exchange.getRequestURI().getQuery()); accept.set(exchange.getRequestHeaders().getFirst("Accept"));
            exchange.getResponseHeaders().set("Content-Type","application/vnd.api+json");
            exchange.sendResponseHeaders(200,sample.length); exchange.getResponseBody().write(sample); exchange.close();
        });
        server.start();
        try {
            var client = new KitsuClient("http://127.0.0.1:"+server.getAddress().getPort()+"/api/edge");
            var page = client.fetchPage(3);
            assertThat(query.get()).isEqualTo("sort=id&page[limit]=10&page[offset]=20");
            assertThat(accept.get()).isEqualTo("application/vnd.api+json");
            assertThat(page.data()).hasSize(10);
            assertThat(page.data().getFirst().id()).isEqualTo("7442");
            assertThat(client.hasNext(page)).isTrue();
        } finally { server.stop(0); }
    }

    @Test void resolvesExternalMalMappingAndRetriesRateLimits() throws Exception {
        var calls = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/anime/7442/mappings", exchange -> {
            int status = calls.incrementAndGet() == 1 ? 429 : 200;
            byte[] body = """
                    {"data":[{"type":"mappings","attributes":{"externalSite":"anilist/anime","externalId":"123"}},
                    {"type":"mappings","attributes":{"externalSite":"myanimelist/anime","externalId":"16498"}}],"links":{"next":null}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/vnd.api+json");
            exchange.sendResponseHeaders(status,body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            assertThat(new KitsuClient("http://127.0.0.1:"+server.getAddress().getPort()).findMalId("7442")).isEqualTo(16498);
            assertThat(calls).hasValue(2);
        } finally { server.stop(0); }
    }

    @Test void rejectsMalformedPagesAndDoesNotRetryClientErrors() throws Exception {
        var calls = new AtomicInteger();
        var response = new AtomicReference<>("{\"data\":[{\"id\":\"0\",\"type\":\"anime\",\"attributes\":{}}]}");
        var status = new AtomicInteger(200);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/anime", exchange -> {
            calls.incrementAndGet(); byte[] body = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/vnd.api+json");
            exchange.sendResponseHeaders(status.get(),body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            var client = new KitsuClient("http://127.0.0.1:"+server.getAddress().getPort());
            assertThatThrownBy(() -> client.fetchPage(1)).hasMessageContaining("identity");
            response.set("{\"data\":[],\"links\":{\"next\":\"next\"}}");
            assertThatThrownBy(() -> client.fetchPage(1)).hasMessageContaining("Invalid Kitsu page");
            response.set("{\"data\":[],\"links\":{\"next\":null}}");
            assertThat(client.hasNext(client.fetchPage(1))).isFalse();
            status.set(400);
            assertThatThrownBy(() -> client.fetchPage(1)).isInstanceOf(org.springframework.web.client.HttpClientErrorException.class);
            assertThat(calls).hasValue(4);
        } finally { server.stop(0); }
    }
    @Test void singleImportResolvesMalIdBeforeFetchingKitsuId() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        var query = new AtomicReference<String>();
        var calls = new AtomicInteger();
        server.createContext("/mappings", exchange -> {
            query.set(exchange.getRequestURI().getQuery());
            byte[] body = """
                    {"data":[{"type":"mappings","attributes":{"externalSite":"myanimelist/anime","externalId":"16498"},
                    "relationships":{"item":{"data":{"type":"anime","id":"7442"}}}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/vnd.api+json");
            exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.createContext("/anime/7442", exchange -> {
            calls.incrementAndGet();
            byte[] body = """
                    {"data":{"id":"7442","type":"anime","attributes":{"canonicalTitle":"Attack on Titan"}}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/vnd.api+json");
            exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            var anime = new KitsuClient("http://127.0.0.1:"+server.getAddress().getPort()).fetchByMalId(16498);
            assertThat(anime.id()).isEqualTo("7442");
            assertThat(query.get()).contains("filter[externalSite]=myanimelist/anime", "filter[externalId]=16498", "include=item");
            assertThat(calls).hasValue(1);
        } finally { server.stop(0); }
    }

    @Test void rejectsConflictingMappingsAndAllowsAnUnmappedRecord() throws Exception {
        var body = new AtomicReference<>("""
                {"data":[{"type":"mappings","attributes":{"externalSite":"myanimelist/anime","externalId":"1"}},
                {"type":"mappings","attributes":{"externalSite":"myanimelist/anime","externalId":"2"}}]}
                """);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/anime/7442/mappings", exchange -> {
            byte[] bytes = body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/vnd.api+json");
            exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            var client = new KitsuClient("http://127.0.0.1:"+server.getAddress().getPort());
            assertThatThrownBy(() -> client.findMalId("7442")).hasMessageContaining("Conflicting MAL mappings");
            body.set("{\"data\":[]}");
            assertThat(client.findMalId("7442")).isNull();
            body.set("{\"data\":null}");
            assertThatThrownBy(() -> client.findMalId("7442")).hasMessageContaining("Invalid Kitsu mappings");
        } finally { server.stop(0); }
    }
}
