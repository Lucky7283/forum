package az.animeazerbaycan.entity;

import az.animeazerbaycan.entity.Enum.WatchlistStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "watchlist_entry")
@Getter
@Setter
public class WatchlistEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anime_id")
    private AnimeTranslated anime;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WatchlistStatus status;
    @Column(nullable = false)
    private Integer episodesWatched;
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
