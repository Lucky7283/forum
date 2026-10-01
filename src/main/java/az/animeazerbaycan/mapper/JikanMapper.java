package az.animeazerbaycan.mapper;

import az.animeazerbaycan.dto.jikan.JikanAnime;
import az.animeazerbaycan.dto.jikan.JikanImage;
import az.animeazerbaycan.dto.jikan.JikanImages;
import az.animeazerbaycan.dto.jikan.JikanReference;
import az.animeazerbaycan.entity.*;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;

@Component
public class JikanMapper {
    public void apply(JikanAnime source, Anime target) {
        String title = title(source);
        target.setMalId(source.malId());
        target.setTitle(title);
        target.setSynopsis(source.synopsis());
        if (target.getSlug() == null)
            target.setSlug(slug(title) + "-" + source.malId());
        target.setPictureUrl(picture(source.images()));
        target.setStartDate(source.aired() == null ? null : date(source.aired().from()));
        target.setEndDate(source.aired() == null ? null : date(source.aired().to()));
        target.setMalMean(source.score());
        target.setMediaType(source.type() == null ? null : source.type().toLowerCase(Locale.ROOT));
        target.setNumEpisodes(source.episodes());
        target.setStudio(source.studios() == null ? null
                : String.join(", ",
                        source.studios().stream().map(JikanReference::name).filter(Objects::nonNull).toList()));
        target.setAiringStatus(source.status() == null ? null : switch (source.status()) {
            case "Finished Airing" -> "finished_airing";
            case "Currently Airing" -> "currently_airing";
            case "Not yet aired" -> "not_yet_aired";
            default -> throw new IllegalArgumentException("Unknown Jikan airing status");
        });
    }

    public String title(JikanAnime source) {
        if (source.title() != null && !source.title().isBlank())
            return source.title();
        if (source.titles() != null)
            for (var value : source.titles())
                if (value != null && "Default".equals(value.type()) && value.title() != null
                        && !value.title().isBlank())
                    return value.title();
        if (source.titleEnglish() != null && !source.titleEnglish().isBlank())
            return source.titleEnglish();
        if (source.titleJapanese() != null && !source.titleJapanese().isBlank())
            return source.titleJapanese();
        throw new IllegalArgumentException("Jikan anime has no usable title");
    }

    private String picture(JikanImages images) {
        if (images == null)
            return null;
        for (var image : new JikanImage[] { images.jpg(), images.webp() }) {
            if (image == null)
                continue;
            for (String url : new String[] { image.largeImageUrl(), image.imageUrl(), image.smallImageUrl() })
                if (url != null && !url.isBlank())
                    return url;
        }
        return null;
    }

    public String slug(String title) {
        String s = title.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("^-|-$", "");
        return s.isEmpty() ? "anime" : s;
    }

    private LocalDate date(String value) {
        return value == null ? null : OffsetDateTime.parse(value).toLocalDate();
    }
}
