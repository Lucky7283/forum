package az.animeazerbaycan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommentRequest(@NotBlank @Size(max = 2000) String body,
        @Positive @Schema(description = "ID of an existing, non-deleted comment on the same anime. Omit or use null for a top-level comment.")
        Long parentCommentId) {
    public CommentRequest(String body) {
        this(body, null);
    }
}
