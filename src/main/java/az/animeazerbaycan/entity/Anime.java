package az.animeazerbaycan.entity;

import az.animeazerbaycan.entity.Enum.TranslateStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "anime")
@Getter
@Setter
public class Anime extends AnimeFields {
    @Column(nullable = false, unique = true, length = 550)
    private String slug;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 30)
    private TranslateStatus translationStatus = TranslateStatus.NOT_TRANSLATED;
}
