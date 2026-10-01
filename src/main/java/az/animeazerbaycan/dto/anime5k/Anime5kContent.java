package az.animeazerbaycan.dto.anime5k;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Anime5kContent(String synopsis, Anime5kAired aired,
                      @JsonProperty("related_entries") List<String> relatedEntries,
                      List<String> characters,
                      @JsonProperty("voice_actors") Map<String, String> voiceActors) {
}
