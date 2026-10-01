package az.animeazerbaycan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "external_links")
@Getter
@Setter
public class ExternalLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anime_id")
    private AnimeTranslated anime;
    @Column(nullable = false)
    private String label;
    @Column(nullable = false, columnDefinition = "text")
    private String url;
}
