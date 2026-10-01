package az.animeazerbaycan.repository;

import az.animeazerbaycan.entity.Comment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
            select c from Comment c where c.anime.id = :animeId and c.parent is null
            and (c.deletedAt is null or exists (select child.id from Comment child where child.parent = c))
            """)
    Page<Comment> findRootComments(@Param("animeId") Long animeId, Pageable pageable);

    @Query("""
            select c from Comment c where c.parent.id = :parentId
            and (c.deletedAt is null or exists (select child.id from Comment child where child.parent = c))
            """)
    Page<Comment> findReplies(@Param("parentId") Long parentId, Pageable pageable);

    // Serialize reply creation against soft deletion of the same parent.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comment c where c.id = :id")
    Optional<Comment> findByIdForUpdate(@Param("id") Long id);
}
