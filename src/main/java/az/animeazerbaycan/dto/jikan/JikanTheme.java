package az.animeazerbaycan.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanTheme(List<String> openings, List<String> endings) {
}
