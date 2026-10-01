package az.animeazerbaycan.animeimporter;

import az.animeazerbaycan.dto.jikan.JikanAnime;
import az.animeazerbaycan.dto.jikan.JikanPageResponse;
import az.animeazerbaycan.dto.jikan.JikanResponse;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import az.animeazerbaycan.common.ApiException;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class JikanClient {
    private final RestClient client;

    public JikanClient(@Value("${app.jikan.base-url}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public JikanPageResponse fetchPage(int page) {
        if (page < 1)
            throw new IllegalArgumentException("Page must be positive");
        var response = client.get().uri(page == 1 ? "/anime" : "/anime?page=" + page)
                .retrieve().body(JikanPageResponse.class);
        if (response == null || response.data() == null || response.pagination() == null
                || response.pagination().hasNextPage() == null
                || !Integer.valueOf(page).equals(response.pagination().currentPage())
                || (response.data().isEmpty() && response.pagination().hasNextPage()))
            throw new IllegalStateException("Invalid Jikan page response");
        for (var anime : response.data()) {
            if (anime == null || anime.malId() == null || anime.malId() < 1)
                throw new IllegalStateException("Invalid anime in Jikan page " + page);
        }
        return response;
    }

    public JikanAnime fetch(int malId) {
        if (malId < 1)
            throw new ApiException(400, "MAL ID must be positive");
        var response = client.get().uri("/anime/{id}/full", malId).retrieve().body(JikanResponse.class);
        if (response == null || response.data() == null || !Integer.valueOf(malId).equals(response.data().malId()))
            throw new IllegalStateException("Invalid Jikan response");
        return response.data();
    }
}
