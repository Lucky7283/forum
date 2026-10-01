package az.animeazerbaycan.repository;

import az.animeazerbaycan.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;

import java.util.*;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByUserIdAndAnimeId(Long userId, Long animeId);

    void deleteByUserIdAndAnimeId(Long userId, Long animeId);

    @Query("select avg(r.score) from Rating r where r.anime.id=:id")
    Double average(@org.springframework.data.repository.query.Param("id") Long id);

    long countByAnimeId(Long animeId);
}
