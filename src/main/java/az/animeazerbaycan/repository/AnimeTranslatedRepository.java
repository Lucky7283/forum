package az.animeazerbaycan.repository;

import az.animeazerbaycan.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;

import java.util.*;

public interface AnimeTranslatedRepository extends JpaRepository<AnimeTranslated, Long>, JpaSpecificationExecutor<AnimeTranslated> {
    Optional<AnimeTranslated> findByMalId(Integer malId);
    @Query(value = """
        SELECT a.*
        FROM anime_translated a
        LEFT JOIN external_links el ON el.anime_id = a.id
        WHERE el.id IS NULL
        ORDER BY a.id
        """, nativeQuery = true)
    List<AnimeTranslated> findAllWithoutExternalLinks();
}
