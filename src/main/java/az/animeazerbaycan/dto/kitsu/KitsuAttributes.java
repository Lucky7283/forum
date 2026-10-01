package az.animeazerbaycan.dto.kitsu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KitsuAttributes(String canonicalTitle, Map<String, String> titles, String synopsis,
                         String description, KitsuImage posterImage, String startDate, String endDate,
                         String subtype, String status, Integer episodeCount) {
}
