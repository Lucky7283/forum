package az.animeazerbaycan.controller;

import az.animeazerbaycan.dto.request.CommentRequest;
import az.animeazerbaycan.dto.request.RatingRequest;
import az.animeazerbaycan.dto.request.WatchlistRequest;
import az.animeazerbaycan.dto.response.AnimeResponse;
import az.animeazerbaycan.dto.response.CommentResponse;
import az.animeazerbaycan.dto.response.PagedResponse;
import az.animeazerbaycan.dto.response.RatingResponse;
import az.animeazerbaycan.dto.response.WatchlistResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import az.animeazerbaycan.entity.Enum.WatchlistStatus;
import az.animeazerbaycan.service.Interface.CommunityService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;

@Tag(name = "Community", description = "User ratings, favorites, watchlists, and anime comments.")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService service;

    @Operation(summary = "Set anime rating", description = "Creates or updates the authenticated user rating. animeId is the internal translated card ID returned by the catalog, not the MAL ID.")
    @PutMapping("/anime/{animeId}/rating")
    public RatingResponse rating(@AuthenticationPrincipal Long userId,
            @PathVariable @Positive Long animeId,
            @Valid @RequestBody RatingRequest r) {
        return service.rate(userId, animeId, r);
    }

    @Operation(summary = "Remove anime rating", description = "Removes the authenticated user rating for an existing translated card. Returns no content even if no rating exists. animeId is the internal card ID.")
    @DeleteMapping("/anime/{animeId}/rating")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRating(@AuthenticationPrincipal Long userId, @PathVariable @Positive Long animeId) {
        service.removeRating(userId, animeId);
    }

    @Operation(summary = "My favorite anime", description = "Returns the authenticated user favorites, most recently added first. Pagination is zero-based; size must be between 1 and 100.")
    @GetMapping("/users/me/favorites")
    public PagedResponse<AnimeResponse> favorites(@AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.favorites(userId, page, size);
    }

    @Operation(summary = "Add favorite anime", description = "Adds an existing translated card to the authenticated user favorites. Repeated requests do not create duplicates. animeId is the internal card ID, not the MAL ID.")
    @PutMapping("/users/me/favorites/{animeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void favorite(@AuthenticationPrincipal Long userId, @PathVariable @Positive Long animeId) {
        service.favorite(userId, animeId);
    }

    @Operation(summary = "Remove favorite anime", description = "Removes an existing translated card from the authenticated user favorites. Returns no content even if it was not a favorite. animeId is the internal card ID.")
    @DeleteMapping("/users/me/favorites/{animeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavorite(@AuthenticationPrincipal Long userId, @PathVariable @Positive Long animeId) {
        service.removeFavorite(userId, animeId);
    }

    @Operation(summary = "My watchlist", description = "Returns the authenticated user watchlist, most recently updated first, optionally filtered by watch status. Pagination is zero-based; size must be between 1 and 100.")
    @GetMapping("/users/me/watchlist")
    public PagedResponse<WatchlistResponse> watchlist(@AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) WatchlistStatus status) {
        return service.watchlist(userId, page, size, status);
    }

    @Operation(summary = "Set watchlist entry", description = "Creates or updates the authenticated user watch status and episodes watched for a translated card. animeId is the internal card ID, not the MAL ID.")
    @PutMapping("/users/me/watchlist/{animeId}")
    public WatchlistResponse watch(@AuthenticationPrincipal Long userId, @PathVariable @Positive Long animeId,
            @Valid @RequestBody WatchlistRequest r) {
        return service.watch(userId, animeId, r);
    }

    @Operation(summary = "Remove watchlist entry", description = "Removes an existing translated card from the authenticated user watchlist. Returns no content even if no entry exists. animeId is the internal card ID.")
    @DeleteMapping("/users/me/watchlist/{animeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWatch(@AuthenticationPrincipal Long userId, @PathVariable @Positive Long animeId) {
        service.removeWatch(userId, animeId);
    }

    @Operation(summary = "Anime comments", description = "Returns top-level comments only, newest first with ID ascending for ties. Deleted parents with replies remain as placeholders (deleted=true, body=null); deleted leaves are hidden. animeId is the internal translated card ID. Pagination is zero-based; size must be between 1 and 100.")
    @GetMapping("/anime/{animeId}/comments")
    public PagedResponse<CommentResponse> comments(@PathVariable @Positive Long animeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.comments(animeId, page, size);
    }

    @Operation(summary = "Comment replies", description = "Returns direct replies to an existing comment, including a deleted parent. Newest first with ID ascending for ties. Deleted replies with children remain as placeholders; deleted leaves are hidden. No nested objects are returned. Pagination is zero-based; size must be between 1 and 100. A missing parent returns 404.")
    @GetMapping("/comments/{commentId}/replies")
    public PagedResponse<CommentResponse> replies(@PathVariable @Positive Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.replies(commentId, page, size);
    }

    @Operation(summary = "Add anime comment", description = "Creates a comment as the authenticated user. Set parentCommentId to reply to a non-deleted comment on the same anime, including another reply; omit it for a top-level comment. Missing or deleted parents return 404; parents on another anime return 400. The author comes only from authentication. animeId is the internal translated card ID, not the MAL ID.")
    @PostMapping("/anime/{animeId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse comment(@AuthenticationPrincipal Long userId, @PathVariable @Positive Long animeId,
            @Valid @RequestBody CommentRequest r) {
        return service.comment(userId, animeId, r);
    }

    @Operation(summary = "Delete my comment", description = "Soft-deletes a comment without deleting its replies. Parents with replies remain as placeholders with deleted=true and body=null; deleted leaves are hidden. Only the author can delete a comment; repeated deletion by the author returns no content.")
    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal Long userId, @PathVariable @Positive Long commentId) {
        service.deleteComment(userId, commentId);
    }
}
