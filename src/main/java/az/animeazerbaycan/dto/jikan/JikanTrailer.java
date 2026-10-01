package az.animeazerbaycan.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanTrailer(@JsonProperty("youtube_id") String youtubeId, String url,
                      @JsonProperty("embed_url") String embedUrl, JikanTrailerImages images) {
}
