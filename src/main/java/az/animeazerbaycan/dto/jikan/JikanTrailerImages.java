package az.animeazerbaycan.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanTrailerImages(@JsonProperty("image_url") String imageUrl,
                            @JsonProperty("small_image_url") String smallImageUrl,
                            @JsonProperty("medium_image_url") String mediumImageUrl,
                            @JsonProperty("large_image_url") String largeImageUrl,
                            @JsonProperty("maximum_image_url") String maximumImageUrl) {
}
