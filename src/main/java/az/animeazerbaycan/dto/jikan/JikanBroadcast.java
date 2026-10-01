package az.animeazerbaycan.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanBroadcast(String day, String time, String timezone, String string) {
}
