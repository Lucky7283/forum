package az.animeazerbaycan.dto.response;

import java.time.Instant;

public record ProfileResponse(Long id, String username, String avatarUrl, Instant createdAt) {
}
