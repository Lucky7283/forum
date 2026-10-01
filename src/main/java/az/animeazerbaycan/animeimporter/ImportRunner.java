package az.animeazerbaycan.animeimporter;

import az.animeazerbaycan.animeimporter.Implementation.JikanImportService;
import az.animeazerbaycan.animeimporter.Implementation.JsonAnimeImportService;
import az.animeazerbaycan.animeimporter.Implementation.KitsuImportService;
import az.animeazerbaycan.animeimporter.Interface.AnimeImportService;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

/**
 * Selects the configured import provider at startup; none disables importing.
 */
@Component
@Order(0)

public class ImportRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ImportRunner.class);
    private final AnimeImportService service;
    private final int startPage;
    private final String file;
    private final boolean fileMode;
    private final ExternalLinksFilling linkFilling;

    public ImportRunner(JikanImportService jikan, JsonAnimeImportService json,
            KitsuImportService kitsu, ExternalLinksFilling linkFilling,
            @Value("${app.anime-import.provider:none}") String provider,
            @Value("${app.anime-import.start-page:1}") int startPage,
            @Value("${app.anime-import.file:classpath:anime_aggregated.jsonl}") String file) {

        this.service = switch (provider.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "none" -> {
                this.linkFilling = null;
                yield null;
            }
            case "jikan" -> {
                this.linkFilling = null;
                yield jikan;
            }
            case "kitsu" -> {
                this.linkFilling = null;
                yield kitsu;
            }
            case "json" -> {
                this.linkFilling = null;
                yield json;
            }
            case "filling" -> {
                this.linkFilling = linkFilling;
                yield null;
            }
            default -> {
                this.linkFilling = null;
                throw new IllegalArgumentException(
                        "Unknown anime import provider: " + provider + "; use none, jikan, kitsu, json or filling");
            }
        };

        this.startPage = startPage;
        this.file = file;
        this.fileMode = "json".equalsIgnoreCase(provider.trim());
    }

    @Override
    public void run(ApplicationArguments args) {
        if (service == null && linkFilling == null)
            return;
        try {
            if (fileMode) {
                service.importFile(file);
                return;
            }
            if (linkFilling != null) {
                log.info("Filling external links for anime");
                linkFilling.fillExternalLinksForAnime(null);
            } else {
                var result = service.importPages(startPage);
                log.info("Anime import finished: {} originals, last page {}", result.imported(),
                        result.lastCompletedPage());
            }
        } catch (RuntimeException failure) {
            log.error("Anime import stopped; application remains available: {}", failure.getMessage(), failure);
        }
    }
}
