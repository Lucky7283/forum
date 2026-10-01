package az.animeazerbaycan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "comments", indexes = @Index(name = "idx_comments_parent_id", columnList = "parent_id"))
@Getter
@Setter
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anime_id")
    private AnimeTranslated anime;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_comments_parent"))
    private Comment parent;
    @Column(nullable = false, length = 2000)
    private String body;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    private Instant deletedAt;
    
}
