package az.animeazerbaycan.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank @Pattern(regexp = "[\\p{L}0-9_]{3,50}") String username,
                       @NotBlank @Email @Size(max = 32) String email,
                       @NotBlank @Size(min = 8, max = 72) String password) {
}
