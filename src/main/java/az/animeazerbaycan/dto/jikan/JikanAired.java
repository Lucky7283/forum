package az.animeazerbaycan.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanAired(String from, String to, JikanDateRange prop, String string) {
    public JikanAired(String from, String to) {
        this(from, to, null, null);
    }
}
