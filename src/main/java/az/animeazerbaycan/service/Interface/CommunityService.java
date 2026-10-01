package az.animeazerbaycan.service.Interface;

import az.animeazerbaycan.dto.request.CommentRequest;
import az.animeazerbaycan.dto.request.RatingRequest;
import az.animeazerbaycan.dto.request.WatchlistRequest;
import az.animeazerbaycan.dto.response.AnimeResponse;
import az.animeazerbaycan.dto.response.CommentResponse;
import az.animeazerbaycan.dto.response.PagedResponse;
import az.animeazerbaycan.dto.response.RatingResponse;
import az.animeazerbaycan.dto.response.WatchlistResponse;
import az.animeazerbaycan.entity.Enum.WatchlistStatus;

public interface CommunityService {

    RatingResponse rate(Long userId, Long animeId, RatingRequest request);

    void removeRating(Long userId, Long animeId);

    void favorite(Long userId, Long animeId);

    void removeFavorite(Long userId, Long animeId);

    PagedResponse<AnimeResponse> favorites(Long userId, int page, int size);

    WatchlistResponse watch(Long userId, Long animeId, WatchlistRequest request);

    PagedResponse<WatchlistResponse> watchlist(Long userId, int page, int size, WatchlistStatus status);

    void removeWatch(Long userId, Long animeId);

    PagedResponse<CommentResponse> comments(Long animeId, int page, int size);

    PagedResponse<CommentResponse> replies(Long commentId, int page, int size);

    CommentResponse comment(Long userId, Long animeId, CommentRequest request);

    void deleteComment(Long userId, Long commentId);
}
