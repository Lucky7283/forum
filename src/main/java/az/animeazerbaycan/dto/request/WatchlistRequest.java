package az.animeazerbaycan.dto.request;

import az.animeazerbaycan.entity.Enum.WatchlistStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WatchlistRequest(@NotNull WatchlistStatus status, @NotNull @Min(0) Integer episodesWatched) {
}
