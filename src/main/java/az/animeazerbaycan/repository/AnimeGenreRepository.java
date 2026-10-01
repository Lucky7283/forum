package az.animeazerbaycan.repository;

import az.animeazerbaycan.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AnimeGenreRepository extends JpaRepository<AnimeGenre, AnimeGenreId> {
    @Query("select link.genre from AnimeGenre link where link.id.animeMalId = :malId order by link.genre.name")
    List<Genre> findGenresByAnimeMalId(@Param("malId") Integer malId);

    List<AnimeGenre> findByIdAnimeMalId(Integer malId);
}
