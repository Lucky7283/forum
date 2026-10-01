package az.animeazerbaycan.repository;

import az.animeazerbaycan.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;

import java.util.*;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
    Page<Favorite> findByUserId(Long userId, Pageable pageable);
}
