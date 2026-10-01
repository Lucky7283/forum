package az.animeazerbaycan.dto.kitsu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KitsuGenrePage(java.util.List<KitsuGenre> data, KitsuLinks links) {}
