package az.animeazerbaycan.service.Interface;

import az.animeazerbaycan.dto.response.AnimeResponse;
import az.animeazerbaycan.dto.response.GenreResponse;
import az.animeazerbaycan.dto.response.PagedResponse;
import az.animeazerbaycan.entity.AnimeTranslated;

import java.util.List;

public interface CatalogService {


    // Fetches a translated anime by its internal ID or throws {@link az.animeazerbaycan.common.ApiException}.
    AnimeTranslated require(Long id);

    /**
     * Maps a translated anime into its public API representation,
     * resolving slug, average rating and rating count.
     */
    AnimeResponse view(AnimeTranslated a);


    AnimeResponse detail(String slug);

    List<GenreResponse> genres();

    /**
     * Returns a paged, filtered and sorted catalog of anime.
     *
     * @param page   zero-based page index
     * @param size   page size
     * @param q      optional title search term (max 500 chars)
     * @param genre  optional genre slug filter
     * @param status optional airing status
     *               ({@code finished_airing}, {@code currently_airing}, {@code not_yet_aired})
     * @param sort   sort specification, e.g. {@code "title,asc"} or {@code "popularity,desc"}
     */
    PagedResponse<AnimeResponse> catalog(int page, int size, String q,
                                             String genre, String status, String sort);
}