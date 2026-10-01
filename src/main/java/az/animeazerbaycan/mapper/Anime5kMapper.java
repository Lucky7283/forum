package az.animeazerbaycan.mapper;

import az.animeazerbaycan.dto.anime5k.Anime5kDto;
import az.animeazerbaycan.entity.Anime;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Component
public class Anime5kMapper {
    public void validate(Anime5kDto dto) {
        if (dto == null || dto.metadata() == null)
            throw new IllegalArgumentException("Missing meta-data");
        var m = dto.metadata();
        if (m.id() == null || m.id() < 1)
            throw new IllegalArgumentException("Missing positive MAL ID");
        if (m.title() == null || m.title().isBlank() || m.title().length() > 500)
            throw new IllegalArgumentException("Title must contain 1..500 characters");
        if (m.studio() != null && m.studio().length() > 255)
            throw new IllegalArgumentException("Studio exceeds 255 characters");
        if (m.type() != null && m.type().length() > 30)
            throw new IllegalArgumentException("Type exceeds 30 characters");
        if (m.score() != null && (m.score().compareTo(BigDecimal.ZERO) < 0 || m.score().compareTo(BigDecimal.TEN) > 0))
            throw new IllegalArgumentException("Score must be between 0 and 10");
        if (dto.data() != null && dto.data().aired() != null) {
            fullDate(dto.data().aired().from());
            fullDate(dto.data().aired().to());
        }
    }

    public void apply(Anime5kDto dto, Anime target) {
        validate(dto);
        var m = dto.metadata();
        target.setMalId(m.id());
        target.setTitle(m.title().strip());
        if (target.getSlug() == null) {
            String slug = m.title().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("^-|-$",
                    "");
            target.setSlug((slug.isEmpty() ? "anime" : slug) + "-" + m.id());
        }
        if (m.type() != null && !m.type().isBlank())
            target.setMediaType(m.type().toLowerCase(Locale.ROOT));
        if (m.studio() != null && !m.studio().isBlank())
            target.setStudio(m.studio());
        if (m.score() != null)
            target.setMalMean(m.score());
        if (dto.data() != null) {
            if (dto.data().synopsis() != null && !dto.data().synopsis().isBlank())
                target.setSynopsis(dto.data().synopsis());
            if (dto.data().aired() != null) {
                var from = fullDate(dto.data().aired().from());
                var to = fullDate(dto.data().aired().to());
                if (from != null)
                    target.setStartDate(from);
                if (to != null)
                    target.setEndDate(to);
            }
        }
        // Missing fields and partial dates preserve existing metadata and translations.
    }

    private LocalDate fullDate(String value) {
        if (value == null || value.isBlank() || "UNKNOWN".equalsIgnoreCase(value))
            return null;
        try {
            if (value.matches("\\d{4}-\\d{2}")) {
                YearMonth.parse(value);
                return null;
            }
            if (value.matches("\\d{4}")) {
                Year.parse(value);
                return null;
            }
            return LocalDate.parse(value);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Invalid aired date: " + value, error);
        }
    }
}
