package az.animeazerbaycan.service.Implementation;

import az.animeazerbaycan.dto.response.AnimeResponse;
import az.animeazerbaycan.dto.response.GenreResponse;
import az.animeazerbaycan.dto.response.PagedResponse;
import az.animeazerbaycan.service.Interface.CatalogService;

import az.animeazerbaycan.entity.*;
import az.animeazerbaycan.repository.*;
import az.animeazerbaycan.mapper.ResponseMapper;
import az.animeazerbaycan.common.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {
    private final AnimeTranslatedRepository translated;
    private final AnimeRepository originals;
    private final GenreRepository genres;
    private final AnimeGenreRepository animeGenres;
    private final RatingRepository ratings;
    private final ResponseMapper mapper;

    @Override
    public AnimeTranslated require(Long id) {
        return translated.findById(id).orElseThrow(ApiException::missing);
    }

    @Override
    public AnimeResponse view(AnimeTranslated a) {
        return mapper.anime(a, originals.findByMalId(a.getMalId()).orElseThrow(ApiException::missing).getSlug(), ratings.average(a.getId()), ratings.countByAnimeId(a.getId()), animeGenres.findGenresByAnimeMalId(a.getMalId()));
    }

    @Override
    public AnimeResponse detail(String slug) {
        var original = originals.findBySlug(slug).orElseThrow(ApiException::missing);
        return view(translated.findByMalId(original.getMalId()).orElseThrow(ApiException::missing));
    }

    @Override
    public List<GenreResponse> genres() {
        return genres.findAll(Sort.by("name")).stream().map(mapper::genre).toList();
    }

    @Override
    public PagedResponse<AnimeResponse> catalog(int page, int size, String q, String genre, String status, String sort) {
        Pageable pageable = Pages.of(page, size, sort, Set.of("title", "malMean", "createdAt", "startDate", "popularity"));
        if (q != null && q.length() > 500) throw new ApiException(400, "Search is too long");
        if (status != null && !Set.of("finished_airing", "currently_airing", "not_yet_aired").contains(status))
            throw new ApiException(400, "Invalid airing status");
        boolean popularity = sort.startsWith("popularity,");
        Specification<AnimeTranslated> filter = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String term = q.strip().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + term + "%", '!'));
            }
            if (genre != null) {
                var sub = query.subquery(Integer.class);
                var link = sub.from(AnimeGenre.class);
                sub.select(link.get("id").get("animeMalId"))
                        .where(cb.equal(link.join("genre").get("slug"), genre));
                predicates.add(root.get("malId").in(sub));
            }
            if (status != null) predicates.add(cb.equal(root.get("airingStatus"), status));
            if (popularity && query.getResultType() != Long.class && query.getResultType() != long.class) {
                var count = query.subquery(Long.class);
                var rating = count.from(Rating.class);
                count.select(cb.count(rating)).where(cb.equal(rating.get("anime").get("id"), root.get("id")));
                query.orderBy(sort.endsWith("desc") ? cb.desc(count) : cb.asc(count), cb.asc(root.get("id")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        if (popularity) pageable = PageRequest.of(page, size);
        return PagedResponse.of(translated.findAll(filter, pageable).map(this::view));
    }
}
