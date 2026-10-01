package az.animeazerbaycan.repository;

import az.animeazerbaycan.entity.*;
import az.animeazerbaycan.entity.Enum.WatchlistStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;

import java.util.*;

public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntry, Long> {
    Optional<WatchlistEntry> findByUserIdAndAnimeId(Long userId, Long animeId);

    Page<WatchlistEntry> findByUserId(Long userId, Pageable pageable);

    Page<WatchlistEntry> findByUserIdAndStatus(Long userId, WatchlistStatus status, Pageable pageable);

    void deleteByUserIdAndAnimeId(Long userId, Long animeId);
}
