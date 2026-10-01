package az.animeazerbaycan.mapper;

import az.animeazerbaycan.dto.kitsu.KitsuAnime;
import az.animeazerbaycan.entity.Anime;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class KitsuMapper {
    public void apply(KitsuAnime source, int malId, Anime target) {
        if (malId < 1 || source == null || source.attributes() == null)
            throw new IllegalArgumentException("Kitsu anime requires a verified MAL ID and attributes");
        var data = source.attributes();
        Map<String, String> titles = data.titles() == null ? Map.of() : data.titles();
        String title = first(data.canonicalTitle(), titles.get("en_jp"), titles.get("en"),
                titles.get("en_us"), titles.get("ja_jp"));
        if (title == null)
            throw new IllegalArgumentException("Kitsu anime has no usable title");
        target.setMalId(malId);
        target.setTitle(title);
        target.setSynopsis(first(data.synopsis(), data.description()));
        if (target.getSlug() == null) {
            String slug = title.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("^-|-$", "");
            target.setSlug((slug.isEmpty() ? "anime" : slug) + "-" + malId);
        }
        var image = data.posterImage();
        target.setPictureUrl(image == null ? null
                : first(image.original(), image.large(), image.medium(), image.small(), image.tiny()));
        target.setStartDate(date(data.startDate()));
        target.setEndDate(date(data.endDate()));
        target.setMediaType(data.subtype() == null ? null : data.subtype().toLowerCase(Locale.ROOT));
        target.setNumEpisodes(data.episodeCount());
        target.setAiringStatus(data.status() == null ? null : switch (data.status()) {
            case "finished" -> "finished_airing";
            case "current" -> "currently_airing";
            case "tba", "unreleased", "upcoming" -> "not_yet_aired";
            default -> throw new IllegalArgumentException("Unknown Kitsu status: " + data.status());
        });
        // Kitsu ratings are not MAL scores; the sample supplies no studio names.
        // Preserve malMean, studio, translation status and translated relationships.
    }

    private static String first(String... values) {
        return Stream.of(values).filter(v -> v != null && !v.isBlank()).findFirst().orElse(null);
    }

    private static LocalDate date(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
