package az.animeazerbaycan.dto.anime5k;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Anime5kAired(String from, String to) {
}
