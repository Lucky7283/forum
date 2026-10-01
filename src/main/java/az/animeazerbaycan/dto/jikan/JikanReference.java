package az.animeazerbaycan.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanReference(@JsonProperty("mal_id") Integer malId, String name, String type, String url) {
    public JikanReference(Integer malId, String name) {
        this(malId, name, null, null);
    }
}
