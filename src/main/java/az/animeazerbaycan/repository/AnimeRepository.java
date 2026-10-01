package az.animeazerbaycan.repository;

import az.animeazerbaycan.entity.*;
import az.animeazerbaycan.entity.Enum.TranslateStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;

import java.util.*;

public interface AnimeRepository extends JpaRepository<Anime, Long>, AnimeTranslationRepository {
    Optional<Anime> findByMalId(Integer malId);

    Optional<Anime> findBySlug(String slug);

    @Query("""
            select a from Anime a
            where a.id > :afterId and a.translationStatus = :pending
              and a.synopsis is not null and trim(a.synopsis) <> ''
              and not exists (select t.id from AnimeTranslated t where t.malId = a.malId)
            order by a.id
            """)
    List<Anime> findTranslationCandidates(@Param("afterId") long afterId,
            @Param("pending") TranslateStatus pending, Pageable pageable);

    @Query(value = "SELECT synopsis FROM anime LIMIT :n", nativeQuery = true)
    List<String> getFirstNAnimeSynopsis(int n);
}
