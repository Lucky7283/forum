package az.animeazerbaycan.controller;

import az.animeazerbaycan.dto.request.ProfileRequest;
import az.animeazerbaycan.dto.response.ProfileResponse;
import az.animeazerbaycan.dto.response.UserResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import az.animeazerbaycan.service.Interface.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Tag(name = "Profiles", description = "Public user profiles and updates to the current account.")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService users;

    @Operation(summary = "User profile", description = "Returns the public profile for an exact username. Returns not found if the user does not exist.")
    @GetMapping("/{username}")
    public ProfileResponse profile(@PathVariable String username) {
        return users.profile(username);
    }

    @Operation(summary = "Update my profile", description = "Updates the authenticated user profile. Omitted fields are preserved; an explicitly null avatarUrl clears the avatar. Returns the updated account details.")
    @PatchMapping("/me")
    public UserResponse update(@AuthenticationPrincipal Long id, @Valid @RequestBody ProfileRequest r) {
        return users.update(id, r);
    }
}
