package az.animeazerbaycan.repository;

import az.animeazerbaycan.entity.Anime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public class AnimeTranslationRepositoryImpl implements AnimeTranslationRepository {
    private final EntityManager entityManager;

    public AnimeTranslationRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Anime> findFreshByIdForTranslation(Long id) {
        Anime current = entityManager.find(Anime.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (current != null) {
            // Refresh even with OpenEntityManagerInView: an earlier read may be cached.
            entityManager.refresh(current, LockModeType.PESSIMISTIC_WRITE);
        }
        return Optional.ofNullable(current);
    }
}
