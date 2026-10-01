package az.animeazerbaycan;

import az.animeazerbaycan.config.OpenApiConfig;
import az.animeazerbaycan.controller.*;
import az.animeazerbaycan.service.Interface.UserService;
import az.animeazerbaycan.service.Interface.CommunityService;
import az.animeazerbaycan.security.JwtService;
import az.animeazerbaycan.entity.Genre;
import az.animeazerbaycan.repository.GenreRepository;
import az.animeazerbaycan.service.Interface.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CatalogOpenApiTest.DocumentationOnly.class)
@AutoConfigureMockMvc(addFilters = false)
class CatalogOpenApiTest {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
    })
    @Import({CatalogController.class, AuthController.class, CommunityController.class, UserController.class, OpenApiConfig.class})
    static class DocumentationOnly {}

    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @MockitoBean CatalogService catalog;
    @MockitoBean UserService users;
    @MockitoBean CommunityService community;
    @MockitoBean JwtService jwt;
    @MockitoBean GenreRepository genres;

    private JsonNode document() throws Exception {
        String body = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/catalog-openapi.json"), body);
        return JsonMapper.builder().build().readTree(body);
    }

    private JsonNode operation() throws Exception {
        return document().path("paths").path("/api/v1/anime").path("get");
    }

    private Map<String, JsonNode> parameters(JsonNode operation) {
        Map<String, JsonNode> values = new LinkedHashMap<>();
        operation.path("parameters").forEach(p -> values.put(p.path("name").asText(), p));
        return values;
    }

    private Genre genre(String slug) {
        var value = new Genre(); value.setSlug(slug); return value;
    }

    @Test void documentsAllParametersEnumsDefaultsAndBadRequests() throws Exception {
        when(genres.findAll()).thenReturn(List.of(genre("action")));
        var op = operation(); var params = parameters(op);
        assertThat(params.keySet()).containsExactlyInAnyOrder("page", "size", "q", "genre", "status", "sort");
        assertThat(params.get("page").path("schema").path("minimum").asInt()).isZero();
        assertThat(params.get("page").path("schema").path("default").asInt()).isZero();
        assertThat(params.get("size").path("schema").path("minimum").asInt()).isEqualTo(1);
        assertThat(params.get("size").path("schema").path("maximum").asInt()).isEqualTo(100);
        assertThat(params.get("size").path("schema").path("default").asInt()).isEqualTo(20);
        assertThat(params.get("q").path("schema").path("maxLength").asInt()).isEqualTo(500);
        assertThat(params.get("status").path("schema").path("enum").toString())
                .isEqualTo("[\"finished_airing\",\"currently_airing\",\"not_yet_aired\"]");
        var sort = params.get("sort").path("schema");
        assertThat(sort.path("default").asText()).isEqualTo("title,asc");
        assertThat(sort.path("enum").size()).isEqualTo(10);
        for (String field : List.of("title", "malMean", "createdAt", "startDate", "popularity"))
            for (String direction : List.of("asc", "desc"))
                assertThat(sort.path("enum").toString()).contains("\"" + field + "," + direction + "\"");
        assertThat(sort.path("enum").toString()).doesNotContain("recent");
        assertThat(op.path("description").asText()).contains("sort=recent", "createdAt,desc", "startDate,desc");
        assertThat(op.path("summary").asText()).isEqualTo("Anime catalog");
        assertThat(op.toString()).doesNotContainPattern("[А-Яа-яЁё]");
        var error = op.path("responses").path("400").path("content").path("application/json");
        assertThat(error.path("schema").path("$ref").asText()).endsWith("/ErrorResponse");
        assertThat(error.path("examples").size()).isEqualTo(5);
        assertThat(context.getBeansOfType(ApplicationRunner.class)).isEmpty();
        assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
        verifyNoInteractions(catalog);
    }

    @Test void genreDropdownRefreshesFromDatabaseOnEachSpecRequest() throws Exception {
        when(genres.findAll()).thenReturn(List.of(genre("drama"), genre("action"), genre("action")));
        assertThat(parameters(operation()).get("genre").path("schema").path("enum").toString())
                .isEqualTo("[\"action\",\"drama\"]");
        when(genres.findAll()).thenReturn(List.of(genre("fantasy")));
        assertThat(parameters(operation()).get("genre").path("schema").path("enum").toString())
                .isEqualTo("[\"fantasy\"]");
        verify(genres, times(2)).findAll();
    }

    @Test void emptyGenreDictionaryDoesNotInventValuesOrPublishEmptyEnum() throws Exception {
        when(genres.findAll()).thenReturn(List.of());
        var param = parameters(operation()).get("genre");
        assertThat(param.path("schema").has("enum")).isFalse();
        assertThat(param.path("description").asText()).contains("genre dictionary is currently empty");
        assertThat(param.has("example")).isFalse();
    }

    @Test void documentsEveryControllerMappingInEnglish() throws Exception {
        var spec = document();
        var mappings = context.getBean("requestMappingHandlerMapping",
                org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.class);
        int count = 0;
        for (var entry : mappings.getHandlerMethods().entrySet()) {
            if (!entry.getValue().getBeanType().getPackageName().equals("az.animeazerbaycan.controller")) continue;
            for (String path : entry.getKey().getPatternValues()) {
                for (var method : entry.getKey().getMethodsCondition().getMethods()) {
                    var op = spec.path("paths").path(path).path(method.name().toLowerCase(java.util.Locale.ROOT));
                    assertThat(op.path("summary").asText()).as(method + " " + path).isNotBlank();
                    assertThat(op.path("description").asText()).isNotBlank();
                    assertThat(op.path("summary").asText() + op.path("description").asText())
                            .doesNotContainPattern("[А-Яа-яЁё]");
                    assertThat(op.path("tags").size()).isEqualTo(1);
                    count++;
                }
            }
        }
        assertThat(count).isEqualTo(21);
        assertThat(spec.path("paths").path("/api/v1/comments/{commentId}/replies").path("get").path("summary").asText())
                .isEqualTo("Comment replies");
        var schemas = spec.path("components").path("schemas");
        assertThat(schemas.path("CommentRequest").path("properties").has("parentCommentId")).isTrue();
        assertThat(schemas.path("CommentResponse").path("properties").has("parentCommentId")).isTrue();
        assertThat(schemas.path("CommentResponse").path("properties").has("deleted")).isTrue();
        assertThat(spec.path("tags").size()).isEqualTo(4);
        assertThat(spec.path("info").path("title").asText()).isEqualTo("Anime Azerbaijan API");
        assertThat(spec.path("info").path("version").asText()).isEqualTo("v1");
        assertThat(spec.path("info").path("description").asText()).contains("HttpOnly auth cookie");
        var cookie = spec.path("components").path("securitySchemes").path("cookieAuth");
        assertThat(cookie.path("type").asText()).isEqualTo("apiKey");
        assertThat(cookie.path("in").asText()).isEqualTo("cookie");
        assertThat(cookie.path("name").asText()).isEqualTo("auth");
        for (var tag : spec.path("tags")) {
            assertThat(tag.path("description").asText()).isNotBlank().doesNotContainPattern("[А-Яа-яЁё]");
        }
        assertThat(context.getBeansOfType(ApplicationRunner.class)).isEmpty();
        assertThat(context.getBeansOfType(javax.sql.DataSource.class)).isEmpty();
        verifyNoInteractions(catalog, users, community, jwt);
    }
}
