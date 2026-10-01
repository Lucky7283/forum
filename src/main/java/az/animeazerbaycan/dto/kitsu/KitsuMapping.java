package az.animeazerbaycan.dto.kitsu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KitsuMapping(String type, KitsuMappingAttributes attributes, Map<String, KitsuRelationship> relationships) {
}
