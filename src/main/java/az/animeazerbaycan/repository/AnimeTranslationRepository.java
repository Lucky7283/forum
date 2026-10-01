package az.animeazerbaycan.repository;

import az.animeazerbaycan.entity.Anime;

import java.util.Optional;

public interface AnimeTranslationRepository {
    /** Reads fresh state under a write lock; requires an active transaction. */
    Optional<Anime> findFreshByIdForTranslation(Long id);
}
