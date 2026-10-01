package az.animeazerbaycan.dto.response;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommentResponse(Long id, Long animeId, ProfileResponse author,
        @Schema(description = "Comment text; null when deleted. Render a deleted-comment placeholder instead.") String body,
        Instant createdAt,
        @Schema(description = "Direct parent comment ID; null for a top-level comment.") Long parentCommentId,
        @Schema(description = "Whether this comment was deleted. Deleted parents remain as placeholders while they have replies.") boolean deleted) {
}
