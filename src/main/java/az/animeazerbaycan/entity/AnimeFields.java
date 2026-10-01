package az.animeazerbaycan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.*;
import java.math.BigDecimal;

@MappedSuperclass
@Getter
@Setter
public abstract class AnimeFields {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private Integer malId;
    @Column(nullable = false, length = 500)
    private String title;
    @Column(columnDefinition = "text")
    private String synopsis;
    @Column(columnDefinition = "text")
    private String pictureUrl;
    private String studio;
    private LocalDate startDate;
    private LocalDate endDate;
    @Column(precision = 4, scale = 2)
    private BigDecimal malMean;
    @Column(length = 30)
    private String mediaType;
    @Column(length = 40)
    private String airingStatus;
    private Integer numEpisodes;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void create() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void update() {
        updatedAt = Instant.now();
    }
}
