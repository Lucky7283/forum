package az.animeazerbaycan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AnimeGenreId implements Serializable {
    @Column(name = "anime_mal_id", nullable = false)
    private Integer animeMalId;
    @Column(name = "genre_id", nullable = false)
    private Long genreId;
}
