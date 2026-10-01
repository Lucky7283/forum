package az.animeazerbaycan.controller;

import az.animeazerbaycan.dto.response.AnimeResponse;
import az.animeazerbaycan.dto.response.GenreResponse;
import az.animeazerbaycan.dto.response.PagedResponse;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import az.animeazerbaycan.service.Interface.CatalogService;

import java.util.List;
import az.animeazerbaycan.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Catalog", description = "Translated anime cards and the shared genre dictionary.")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {
        private final CatalogService catalog;

        @Operation(summary = "Anime catalog", description = """
                        Paginated catalog of translated anime cards with filters that can be combined.
                        The response contains data and meta; page numbering starts at 0.
                        Equal sort values are ordered by id ascending.
                        sort=recent is NOT supported: use createdAt,desc for recently added cards,
                        or startDate,desc for anime with a later airing start date.
                        Example: /api/v1/anime?page=0&size=20&q=naruto&status=finished_airing&sort=malMean,desc
                        """)
        @ApiResponse(responseCode = "200", description = "Catalog page; no matches returns an empty data array")
        @ApiResponse(responseCode = "400", description = """
                        page < 0; size outside 1..100; q longer than 500; unsupported status or sort;
                        non-integer page or size, or values outside the 32-bit integer range.
                        genre is not validated against the dictionary: an unknown slug returns an empty page, not 400.
                        """, content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class), examples = {
                        @ExampleObject(name = "pagination", value = "{\"status\":400,\"message\":\"Page must be nonnegative and size between 1 and 100\",\"errors\":{}}"),
                        @ExampleObject(name = "sort", value = "{\"status\":400,\"message\":\"Unsupported sort\",\"errors\":{}}"),
                        @ExampleObject(name = "status", value = "{\"status\":400,\"message\":\"Invalid airing status\",\"errors\":{}}"),
                        @ExampleObject(name = "search", value = "{\"status\":400,\"message\":\"Search is too long\",\"errors\":{}}"),
                        @ExampleObject(name = "parameterType", value = "{\"status\":400,\"message\":\"Invalid request\",\"errors\":{}}")
        }))
        @GetMapping("/anime")
        public PagedResponse<AnimeResponse> catalog(
                        @Parameter(description = "Zero-based page number.", schema = @Schema(type = "integer", format = "int32", minimum = "0", defaultValue = "0"), example = "0") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Page size, from 1 to 100.", schema = @Schema(type = "integer", format = "int32", minimum = "1", maximum = "100", defaultValue = "20"), example = "20") @RequestParam(defaultValue = "20") int size,
                        @Parameter(description = """
                                        Case-insensitive substring search in title. Leading and trailing whitespace is stripped;
                                        an empty or whitespace-only value is ignored. Length is checked BEFORE stripping:
                                        at most 500 UTF-16 code units (Java String.length). %, _ and ! are treated literally.
                                        """, schema = @Schema(maxLength = 500), example = "naruto") @RequestParam(required = false) String q,
                        @Parameter(description = """
                                        One exact genre slug from GET /api/v1/genres, not an ID or display name.
                                        All current slugs are loaded from the database into the OpenAPI allowed values.
                                        The filter does not strip whitespace or normalize case. An unknown slug returns an empty page.
                                        Omit this parameter to disable genre filtering.
                                        """) @RequestParam(required = false) String genre,
                        @Parameter(description = "Airing status: finished_airing = finished; currently_airing = airing; not_yet_aired = not started yet. Omit for any status.", schema = @Schema(allowableValues = {
                                        "finished_airing", "currently_airing",
                                        "not_yet_aired" }), example = "finished_airing") @RequestParam(required = false) String status,
                        @Parameter(description = """
                                        One case-sensitive field,direction pair without spaces.
                                        title = anime title; malMean = MAL score; createdAt = translated card creation time;
                                        startDate = airing start date; popularity = COUNT of user ratings, not their average.
                                        asc = ascending, desc = descending; ties use id,asc.
                                        recent is unsupported. Use createdAt,desc for recently added cards or startDate,desc for airing start date.
                                        """, schema = @Schema(defaultValue = "title,asc", allowableValues = {
                                        "title,asc", "title,desc", "malMean,asc", "malMean,desc", "createdAt,asc",
                                        "createdAt,desc",
                                        "startDate,asc", "startDate,desc", "popularity,asc",
                                        "popularity,desc" }), examples = {
                                                        @ExampleObject(name = "recentlyAdded", value = "createdAt,desc"),
                                                        @ExampleObject(name = "latestStartDate", value = "startDate,desc"),
                                                        @ExampleObject(name = "mostRated", value = "popularity,desc")
                                        }) @RequestParam(defaultValue = "title,asc") String sort) {
                return catalog.catalog(page, size, q, genre, status, sort);
        }

        @Operation(summary = "Anime details", description = "Returns a translated anime card by the original anime slug, including shared genres and local rating statistics. Returns not found if the original or its translation is missing.")
        @GetMapping("/anime/{slug}")
        public AnimeResponse detail(@PathVariable String slug) {
                return catalog.detail(slug);
        }

        @Operation(summary = "Genre dictionary", description = "Returns all stored genres ordered by name. Use a returned slug as the anime catalog genre filter.")
        @GetMapping("/genres")
        public List<GenreResponse> genres() {
                return catalog.genres();
        }
}
