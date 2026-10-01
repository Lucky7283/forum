package az.animeazerbaycan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "favorite")
@Getter
@Setter
public class Favorite {
    @EmbeddedId
    private FavoriteId id;
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @MapsId("animeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anime_id")
    private AnimeTranslated anime;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
