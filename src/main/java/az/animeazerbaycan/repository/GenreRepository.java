package az.animeazerbaycan.repository;

import az.animeazerbaycan.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;

import java.util.*;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByMalId(Integer malId);

    Optional<Genre> findBySlug(String slug);

    Optional<Genre> findByNameIgnoreCase(String name);
}
