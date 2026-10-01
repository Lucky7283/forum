package az.animeazerbaycan.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanPagination(@JsonProperty("has_next_page") Boolean hasNextPage,
                         @JsonProperty("current_page") Integer currentPage,
                         @JsonProperty("last_visible_page") Integer lastVisiblePage, JikanItems items) {
    public JikanPagination(Boolean hasNextPage, Integer currentPage) {
        this(hasNextPage, currentPage, null, null);
    }
}
