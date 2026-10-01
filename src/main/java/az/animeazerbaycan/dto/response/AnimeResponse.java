package az.animeazerbaycan.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AnimeResponse(Long id, Integer malId, String slug, String title, String synopsis, String pictureUrl,
                    String studio, LocalDate startDate, LocalDate endDate, BigDecimal malMean, String mediaType,
                    String airingStatus, Integer numEpisodes, Instant createdAt, Instant updatedAt,
                    List<GenreResponse> genres, List<LinkResponse> externalLinks, Double localMean, long ratingCount) {
}
