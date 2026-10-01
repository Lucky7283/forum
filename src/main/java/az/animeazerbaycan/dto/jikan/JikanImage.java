package az.animeazerbaycan.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanImage(@JsonProperty("large_image_url") String largeImageUrl,
                    @JsonProperty("image_url") String imageUrl,
                    @JsonProperty("small_image_url") String smallImageUrl) {
    public JikanImage(String largeImageUrl) {
        this(largeImageUrl, null, null);
    }
}
