package az.animeazerbaycan.animeimporter.Implementation;

import az.animeazerbaycan.service.Interface.AnimeGenreService;

import az.animeazerbaycan.animeimporter.Interface.AnimeImportService;
import az.animeazerbaycan.animeimporter.KitsuClient;
import az.animeazerbaycan.dto.importer.ImportResult;
import az.animeazerbaycan.dto.importer.PageImportResult;
import az.animeazerbaycan.dto.kitsu.KitsuAnime;
import az.animeazerbaycan.entity.Anime;
import az.animeazerbaycan.mapper.KitsuMapper;
import az.animeazerbaycan.repository.AnimeRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;

@Service
@AllArgsConstructor
public class KitsuImportService implements AnimeImportService {
    private static final Logger log = LoggerFactory.getLogger(KitsuImportService.class);
    private final KitsuClient client;
    private final AnimeRepository originals;
    private final KitsuMapper mapper;
    private final TransactionTemplate transactions;
    private final AnimeGenreService genres;

    @Override
    public synchronized PageImportResult importPages(int startPage) {
        if (startPage < 1)
            throw new IllegalArgumentException("Start page must be positive");
        int imported = 0;
        for (int page = startPage; ; page = Math.incrementExact(page)) {
            KitsuClient.checkInterrupted();
            try {
                var response = client.fetchPage(page);
                for (var anime : response.data()) {
                    KitsuClient.pause(1100);
                    Integer malId = client.findMalId(anime.id());
                    if (malId == null) {
                        log.warn("Skipping Kitsu ID {} without a MAL mapping", anime.id());
                        continue;
                    }
                    save(anime, malId);
                    imported++;
                }
                log.info("Imported Kitsu page {}; saved {} originals in this run", page, imported);
                if (!client.hasNext(response))
                    return new PageImportResult(imported, page);
                KitsuClient.pause(1100);
            } catch (RuntimeException failure) {
                throw new IllegalStateException("Kitsu import stopped after saving " + imported
                        + " originals; resume with app.anime-import.start-page=" + page, failure);
            }
        }
    }

    @Override
    public Long importAnime(int malId) {
        var anime = client.fetchByMalId(malId);
        if (anime == null)
            throw new IllegalStateException("No Kitsu mapping for MAL ID " + malId);
        return save(anime, malId);
    }

    @Override
    public synchronized ImportResult importRange(int fromId, int toId) {
        if (fromId < 1 || toId < fromId)
            throw new IllegalArgumentException("Specify positive MAL IDs");
        int imported = 0;
        var skipped = new ArrayList<Integer>();
        for (int id = fromId; ; id++) {
            KitsuClient.checkInterrupted();
            try {
                var anime = client.fetchByMalId(id);
                if (anime == null)
                    skipped.add(id);
                else {
                    save(anime, id);
                    imported++;
                }
            } catch (RuntimeException failure) {
                throw new IllegalStateException("Kitsu import stopped at MAL ID " + id + " after saving " + imported,
                        failure);
            }
            if (id == toId)
                break;
            KitsuClient.pause(1100);
        }
        return new ImportResult(imported, skipped);
    }

    private Long save(KitsuAnime data, int malId) {
        KitsuClient.checkInterrupted();
        KitsuClient.pause(1100);
        var names = client.fetchGenreNames(data.id());
        return transactions.execute(status -> {
            var original = originals.findByMalId(malId).orElseGet(Anime::new);
            mapper.apply(data, malId, original);
            originals.saveAndFlush(original);
            genres.replace(malId, names);
            return original.getId();
        });
    }
}
