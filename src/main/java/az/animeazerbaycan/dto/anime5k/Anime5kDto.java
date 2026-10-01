package az.animeazerbaycan.dto.anime5k;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Anime5kDto(@JsonProperty("meta-data") Anime5kMetadata metadata, Anime5kContent data) {
}
