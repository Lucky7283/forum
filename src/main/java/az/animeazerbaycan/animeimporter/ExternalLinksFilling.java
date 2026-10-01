package az.animeazerbaycan.animeimporter;

import az.animeazerbaycan.entity.Anime;
import az.animeazerbaycan.entity.ExternalLink;
import az.animeazerbaycan.repository.AnimeTranslatedRepository;
import az.animeazerbaycan.repository.ExternalLinkRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ExternalLinksFilling {
    private final static Logger LOGGER = LoggerFactory.getLogger(ExternalLinksFilling.class);
    private final JikanClient client;
    private final AnimeTranslatedRepository animeRepository;
    private final ExternalLinkRepository externalLinkRepository;
    
    public void fillExternalLinksForAnime(Anime anime) {
        // The argument is retained for the existing runner; this operation fills all missing links.
        var candidates = animeRepository.findAllWithoutExternalLinks();
        var visited = new java.util.HashSet<Long>();
        boolean requested = false;
        for (var translated : candidates) {
            if (Thread.currentThread().isInterrupted())
                return;
            if (translated == null || translated.getId() == null || translated.getMalId() == null
                    || translated.getMalId() < 1 || !visited.add(translated.getId()))
                continue;
            try {
                if (requested)
                    Thread.sleep(1000);
                requested = true;
                var data = client.fetch(translated.getMalId());
                if (data == null || !translated.getMalId().equals(data.malId())
                        || data.url() == null || data.url().isBlank()) {
                    LOGGER.warn("No MAL URL returned for anime with MAL ID {}", translated.getMalId());
                    continue;
                }
                String url = data.url().strip();
                var uri = java.net.URI.create(url);
                String path = uri.getPath();
                String expectedPath = "/anime/" + translated.getMalId();
                if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                        || !("myanimelist.net".equalsIgnoreCase(uri.getHost())
                                || "www.myanimelist.net".equalsIgnoreCase(uri.getHost()))
                        || uri.getUserInfo() != null || path == null
                        || !(path.equals(expectedPath) || path.startsWith(expectedPath + "/"))) {
                    LOGGER.warn("Invalid MAL URL returned for anime with MAL ID {}", translated.getMalId());
                    continue;
                }
                var link = new ExternalLink();
                link.setAnime(translated);
                link.setLabel("Mal");
                link.setUrl(url);
                externalLinkRepository.save(link);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException failure) {
                LOGGER.error("Failed to fill MAL link for anime with MAL ID {}", translated.getMalId(), failure);
            }
        }
    }
}
