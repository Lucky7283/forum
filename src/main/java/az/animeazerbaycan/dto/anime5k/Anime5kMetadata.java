package az.animeazerbaycan.dto.anime5k;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Anime5kMetadata(Integer id, String title, String type, List<String> genre,
                       BigDecimal score, Integer rank, Integer popularity, String studio) {
}
