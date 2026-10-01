package az.animeazerbaycan.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanAnime(@JsonProperty("mal_id") Integer malId,
                    String title,
                    String synopsis,
                    JikanImages images,
                    JikanAired aired,
                    BigDecimal score,
                    List<JikanReference> genres,
                    List<JikanReference> studios,
                    String type,
                    String status,
                    Integer episodes,
                    List<JikanLink> external,
                    List<JikanLink> streaming,
                    String url,
                    JikanTrailer trailer,
                    Boolean approved,
                    List<JikanTitle> titles,
                    @JsonProperty("title_english") String titleEnglish,
                    @JsonProperty("title_japanese") String titleJapanese,
                    @JsonProperty("title_synonyms") List<String> titleSynonyms,
                    String source,
                    Boolean airing,
                    String duration,
                    String rating,
                    @JsonProperty("scored_by") Integer scoredBy,
                    Integer rank,
                    Integer popularity,
                    Integer members,
                    Integer favorites,
                    String background,
                    String season,
                    Integer year,
                    JikanBroadcast broadcast,
                    List<JikanReference> producers,
                    List<JikanReference> licensors,
                    @JsonProperty("explicit_genres") List<JikanReference> explicitGenres,
                    List<JikanReference> themes,
                    List<JikanReference> demographics,
                    List<JikanRelation> relations,
                    JikanTheme theme) {
    /**
     * Compatibility constructor for existing fixtures and partial list records.
     */
    public JikanAnime(Integer malId, String title, String synopsis, JikanImages images, JikanAired aired, BigDecimal score, List<JikanReference> genres, List<JikanReference> studios, String type, String status, Integer episodes, List<JikanLink> external, List<JikanLink> streaming) {
        this(malId, title, synopsis, images, aired, score, genres, studios, type, status, episodes, external, streaming, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
