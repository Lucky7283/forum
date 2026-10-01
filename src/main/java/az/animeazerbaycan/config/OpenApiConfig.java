package az.animeazerbaycan.config;

import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;

import az.animeazerbaycan.entity.Genre;
import az.animeazerbaycan.repository.GenreRepository;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import java.util.Objects;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Anime Azerbaijan API", version = "v1", description = "PostgreSQL-backed anime community API. Mutating requests require the configured frontend Origin. Pagination size is 1–100. Login sets an HttpOnly auth cookie."))
@SecurityScheme(name = "cookieAuth", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE, paramName = "auth")
public class OpenApiConfig {
    @Bean
    OpenApiCustomizer catalogGenreValues(GenreRepository genres) {
        return openApi -> {
            var path = openApi.getPaths() == null ? null : openApi.getPaths().get("/api/v1/anime");
            if (path == null || path.getGet() == null || path.getGet().getParameters() == null)
                return;
            // Set explicit defaults on the final schema: springdoc may discard annotation
            // defaults while merging parameter examples with inferred request parameters.
            for (var p : path.getGet().getParameters()) {
                if (!"query".equals(p.getIn()) || p.getSchema() == null)
                    continue;
                switch (p.getName()) {
                    case "page" -> p.getSchema().setDefault(0);
                    case "size" -> p.getSchema().setDefault(20);
                    case "sort" -> p.getSchema().setDefault("title,asc");
                    default -> {
                    }
                }
            }
            var parameter = path.getGet().getParameters().stream()
                    .filter(p -> "genre".equals(p.getName()) && "query".equals(p.getIn()))
                    .findFirst().orElse(null);
            if (parameter == null)
                return;
            var slugs = genres.findAll().stream().map(Genre::getSlug)
                    .filter(Objects::nonNull).filter(s -> !s.isBlank()).distinct().sorted().toList();
            var schema = new StringSchema();
            if (slugs.isEmpty()) {
                parameter.setDescription(parameter.getDescription()
                        + " The genre dictionary is currently empty; no values are available.");
            } else {
                schema.setEnum(slugs);
                parameter.setExample(slugs.getFirst());
            }
            parameter.setSchema(schema);
        };
    }
}
