package az.animeazerbaycan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * One shared genre membership for an anime's MAL identity, independent of translation.
 */
@Entity
@Table(name = "anime_genre_mal")
@Getter
@Setter
public class AnimeGenre {
    @EmbeddedId
    private AnimeGenreId id;

    @MapsId("genreId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genre_id", nullable = false)
    private Genre genre;
}
