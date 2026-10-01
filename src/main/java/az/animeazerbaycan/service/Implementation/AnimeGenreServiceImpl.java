package az.animeazerbaycan.service.Implementation;

import az.animeazerbaycan.entity.*;
import az.animeazerbaycan.repository.*;
import az.animeazerbaycan.service.Interface.AnimeGenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnimeGenreServiceImpl implements AnimeGenreService {
    private final GenreRepository genres;
    private final AnimeGenreRepository links;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void replace(Integer animeMalId, List<String> names) {
        if (animeMalId == null || animeMalId < 1)
            throw new IllegalArgumentException("A positive anime MAL ID is required");
        if (names == null) return;
        Map<String, String> normalized = new LinkedHashMap<>();
        for (String value : names) {
            if (value == null || value.isBlank())
                throw new IllegalArgumentException("Genre name must not be blank");
            String name = Normalizer.normalize(value, Normalizer.Form.NFKC).strip().replaceAll("\\s+", " ");
            String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("^-|-$", "");
            if (slug.isEmpty() || name.length() > 255 || slug.length() > 255)
                throw new IllegalArgumentException("Invalid genre name: " + name);
            normalized.putIfAbsent(slug, name);
        }
        Map<Long, Genre> desired = new LinkedHashMap<>();
        for (var entry : normalized.entrySet()) {
            var genre = genres.findByNameIgnoreCase(entry.getValue())
                    .or(() -> genres.findBySlug(entry.getKey())).orElseGet(() -> {
                        var created = new Genre();
                        created.setName(entry.getValue());
                        created.setSlug(entry.getKey());
                        return genres.saveAndFlush(created);
                    });
            desired.put(genre.getId(), genre);
        }
        Set<Long> existing = new HashSet<>();
        for (var link : links.findByIdAnimeMalId(animeMalId)) {
            Long genreId = link.getId().getGenreId();
            if (desired.containsKey(genreId)) existing.add(genreId);
            else links.delete(link);
        }
        for (var genre : desired.values()) {
            if (existing.contains(genre.getId())) continue;
            var link = new AnimeGenre();
            link.setId(new AnimeGenreId(animeMalId, genre.getId()));
            link.setGenre(genre);
            links.save(link);
        }
    }
}
