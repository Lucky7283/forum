package az.animeazerbaycan.controller;

import az.animeazerbaycan.dto.request.LoginRequest;
import az.animeazerbaycan.dto.request.RegisterRequest;
import az.animeazerbaycan.dto.response.UserResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import az.animeazerbaycan.service.Interface.UserService;
import az.animeazerbaycan.security.JwtService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Tag(name = "Authentication", description = "Account registration, sign-in, sign-out, and the current user.")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService users;
    private final JwtService jwt;

    @Operation(summary = "Register account", description = "Creates an account and returns its user details. Registration does not sign the user in; use the login endpoint to receive an authentication cookie.")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest r) {
        return users.register(r);
    }

    @Operation(summary = "Sign in", description = "Validates email and password, returns user details, and sets the auth cookie with HttpOnly, Secure, and SameSite=Strict attributes.")
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest r) {
        var u = users.login(r);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(jwt.issue(u.id()), jwt.ttl()).toString()).body(u);
    }

    @Operation(summary = "Sign out", description = "Requires authentication. Clears the auth cookie and returns no content.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", java.time.Duration.ZERO).toString()).build();
    }

    @Operation(summary = "Current user", description = "Returns account details for the authenticated user.")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Long id) {
        return users.me(id);
    }

    private ResponseCookie cookie(String value,
                                  java.time.Duration ttl) {
        return ResponseCookie.from("auth", value).httpOnly(true).secure(true).sameSite("Strict").path("/").maxAge(ttl).build();
    }
}
