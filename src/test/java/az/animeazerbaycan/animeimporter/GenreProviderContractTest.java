package az.animeazerbaycan.animeimporter;

import az.animeazerbaycan.animeimporter.Implementation.*;
import az.animeazerbaycan.dto.jikan.*;
import az.animeazerbaycan.mapper.*;
import az.animeazerbaycan.repository.*;
import az.animeazerbaycan.service.Interface.AnimeGenreService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GenreProviderContractTest {
    @TempDir Path directory;
    private final AnimeGenreService genres = mock(AnimeGenreService.class);
    private final AnimeRepository originals = mock(AnimeRepository.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private TransactionTemplate transactions() {
        when(manager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        return new TransactionTemplate(manager);
    }

    @Test void jikanPageImportSavesGenresWithoutAnyTranslatedCard() {
        var client = mock(JikanClient.class);
        var translated = mock(AnimeTranslatedRepository.class);
        var data = new JikanAnime(42, "Title", null, null, null, null,
                List.of(new JikanReference(1, "Action")), null, null, null, null, null, null);
        when(client.fetchPage(1)).thenReturn(new JikanPageResponse(List.of(data), new JikanPagination(false, 1)));
        when(client.fetch(42)).thenReturn(data);
        var service = new JikanImportService(client, originals, translated, genres, new JikanMapper(), transactions());
        assertThat(service.importPages(1).imported()).isEqualTo(1);
        verify(genres).replace(42, List.of("Action"));
        verifyNoInteractions(translated);
        verify(manager).commit(any());
    }

    @Test void jsonGenreNamesAreSavedAndFailureRollsBackAnimeTransaction() throws Exception {
        Path file = directory.resolve("anime.jsonl");
        Files.writeString(file, """
                {"meta-data":{"id":42,"title":"Title","genre":["Action","Drama"]}}
                """);
        var service = new JsonAnimeImportService(originals, new Anime5kMapper(), transactions(), genres, file.toString());
        service.importFile(file.toString());
        verify(genres).replace(42, List.of("Action", "Drama"));
        doThrow(new IllegalStateException("genre failure")).when(genres).replace(any(), any());
        assertThatThrownBy(() -> service.importFile(file.toString())).hasMessageContaining("after saving 0");
        verify(manager).rollback(any());
    }

    

    @Test void kitsuGenresAreFetchedOutsideTransactionAndFailureDoesNotWrite() {
        var client = mock(KitsuClient.class);
        var data = new az.animeazerbaycan.dto.kitsu.KitsuAnime("7", "anime",
                new az.animeazerbaycan.dto.kitsu.KitsuAttributes("Title", null, null, null, null, null, null, null, null, null));
        when(client.fetchByMalId(42)).thenReturn(data);
        when(client.fetchGenreNames("7")).thenReturn(List.of("Action"));
        var service = new KitsuImportService(client, originals, new KitsuMapper(), transactions(), genres);
        service.importAnime(42);
        var order = inOrder(client, manager, genres);
        order.verify(client).fetchByMalId(42);
        order.verify(client).fetchGenreNames("7");
        order.verify(manager).getTransaction(any());
        order.verify(genres).replace(42, List.of("Action"));
        clearInvocations(originals, manager, genres);
        when(client.fetchGenreNames("7")).thenThrow(new IllegalStateException("genres unavailable"));
        assertThatThrownBy(() -> service.importAnime(42)).hasMessageContaining("genres unavailable");
        verifyNoInteractions(originals, manager, genres);
    }

    @Test void kitsuGenreEndpointPaginatesAndRejectsMalformedResponse() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var requests = new ArrayList<String>();
        server.createContext("/anime/7/genres", exchange -> {
            String query = exchange.getRequestURI().getQuery(); requests.add(query);
            String body = query.contains("offset]=0")
                    ? "{\"data\":[{\"id\":\"1\",\"type\":\"genres\",\"attributes\":{\"name\":\"Action\"}}],\"links\":{\"next\":\"next-page\"}}"
                    : "{\"data\":[{\"id\":\"2\",\"type\":\"genres\",\"attributes\":{\"name\":\"Drama\"}}],\"links\":{\"next\":null}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/vnd.api+json");
            exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.createContext("/anime/8/genres", exchange -> {
            byte[] bytes = "{\"data\":null}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            var client = new KitsuClient("http://127.0.0.1:" + server.getAddress().getPort());
            assertThat(client.fetchGenreNames("7")).containsExactly("Action", "Drama");
            assertThat(requests).containsExactly("page[limit]=20&page[offset]=0", "page[limit]=20&page[offset]=20");
            assertThatThrownBy(() -> client.fetchGenreNames("8")).hasMessageContaining("Invalid Kitsu genres response");
        } finally { server.stop(0); }
    }

    
}
