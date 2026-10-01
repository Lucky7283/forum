package az.animeazerbaycan.dto.response;

import az.animeazerbaycan.entity.Enum.WatchlistStatus;
import java.time.Instant;

public record WatchlistResponse(AnimeResponse anime, WatchlistStatus status, Integer episodesWatched, Instant updatedAt) {
}
