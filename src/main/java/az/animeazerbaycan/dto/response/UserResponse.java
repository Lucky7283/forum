package az.animeazerbaycan.dto.response;

import java.time.Instant;

public record UserResponse(Long id, String username, String email, String avatarUrl, String role, Instant createdAt) {
}
