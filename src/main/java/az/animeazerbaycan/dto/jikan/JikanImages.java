package az.animeazerbaycan.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanImages(JikanImage jpg, JikanImage webp) {
    public JikanImages(JikanImage jpg) {
        this(jpg, null);
    }
}
