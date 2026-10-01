package az.animeazerbaycan.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanDateRange(JikanDateParts from, JikanDateParts to) {
}
