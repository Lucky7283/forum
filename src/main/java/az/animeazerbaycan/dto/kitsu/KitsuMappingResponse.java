package az.animeazerbaycan.dto.kitsu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KitsuMappingResponse(List<KitsuMapping> data, KitsuLinks links) {
}
