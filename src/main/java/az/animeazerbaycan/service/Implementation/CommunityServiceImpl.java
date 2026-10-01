package az.animeazerbaycan.service.Implementation;

import az.animeazerbaycan.dto.request.CommentRequest;
import az.animeazerbaycan.dto.request.RatingRequest;
import az.animeazerbaycan.dto.request.WatchlistRequest;
import az.animeazerbaycan.dto.response.AnimeResponse;
import az.animeazerbaycan.dto.response.CommentResponse;
import az.animeazerbaycan.dto.response.PagedResponse;
import az.animeazerbaycan.dto.response.RatingResponse;
import az.animeazerbaycan.dto.response.WatchlistResponse;
import az.animeazerbaycan.service.Interface.CommunityService;

import az.animeazerbaycan.entity.*;
import az.animeazerbaycan.entity.Enum.WatchlistStatus;
import az.animeazerbaycan.repository.*;
import az.animeazerbaycan.mapper.ResponseMapper;
import az.animeazerbaycan.common.*;
import az.animeazerbaycan.service.Interface.CatalogService;
import az.animeazerbaycan.service.Interface.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityServiceImpl implements CommunityService {
    private final UserService users;
    private final CatalogService catalog;
    private final RatingRepository ratings;
    private final FavoriteRepository favorites;
    private final WatchlistEntryRepository watchlist;
    private final CommentRepository comments;
    private final ResponseMapper mapper;

    @Override
    public RatingResponse rate(Long userId, Long animeId, RatingRequest request) {
        User user = users.lock(userId);
        var anime = catalog.require(animeId);
        var r = ratings.findByUserIdAndAnimeId(userId, animeId).orElseGet(Rating::new);
        r.setUser(user);
        r.setAnime(anime);
        r.setScore(request.localScore());
        ratings.save(r);
        return new RatingResponse(animeId, r.getScore());
    }

    @Override
    public void removeRating(Long userId, Long animeId) {
        users.lock(userId);
        catalog.require(animeId);
        ratings.deleteByUserIdAndAnimeId(userId, animeId);
    }

    @Override
    public void favorite(Long userId, Long animeId) {
        User u = users.lock(userId);
        var a = catalog.require(animeId);
        var id = new FavoriteId(userId, animeId);
        if (!favorites.existsById(id)) {
            Favorite f = new Favorite();
            f.setId(id);
            f.setUser(u);
            f.setAnime(a);
            favorites.save(f);
        }
    }

    @Override
    public void removeFavorite(Long userId, Long animeId) {
        users.lock(userId);
        catalog.require(animeId);
        favorites.findById(new FavoriteId(userId, animeId)).ifPresent(favorites::delete);
    }

    @Transactional(readOnly = true)
    @Override
    public PagedResponse<AnimeResponse> favorites(Long userId, int page, int size) {
        return PagedResponse.of(favorites.findByUserId(userId, org.springframework.data.domain.PageRequest.of(validPage(page, size), size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt").and(org.springframework.data.domain.Sort.by("id.animeId")))).map(f -> catalog.view(f.getAnime())));
    }

    private int validPage(int page, int size) {
        Pages.of(page, size, "createdAt,desc", Set.of("createdAt"));
        return page;
    }

    @Override
    public WatchlistResponse watch(Long userId, Long animeId, WatchlistRequest request) {
        User u = users.lock(userId);
        var a = catalog.require(animeId);
        var w = watchlist.findByUserIdAndAnimeId(userId, animeId).orElseGet(WatchlistEntry::new);
        w.setUser(u);
        w.setAnime(a);
        w.setStatus(request.status());
        w.setEpisodesWatched(request.episodesWatched());
        w.setUpdatedAt(Instant.now());
        watchlist.save(w);
        return watchView(w);
    }

    private WatchlistResponse watchView(WatchlistEntry w) {
        return new WatchlistResponse(catalog.view(w.getAnime()), w.getStatus(), w.getEpisodesWatched(), w.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    @Override
    public PagedResponse<WatchlistResponse> watchlist(Long userId, int page, int size, WatchlistStatus status) {
        var pageable = Pages.of(page, size, "updatedAt,desc", Set.of("updatedAt"));
        return PagedResponse.of((status == null ? watchlist.findByUserId(userId, pageable) : watchlist.findByUserIdAndStatus(userId, status, pageable)).map(this::watchView));
    }

    @Override
    public void removeWatch(Long userId, Long animeId) {
        users.lock(userId);
        catalog.require(animeId);
        watchlist.deleteByUserIdAndAnimeId(userId, animeId);
    }

    @Transactional(readOnly = true)
    @Override
    public PagedResponse<CommentResponse> comments(Long animeId, int page, int size) {
        catalog.require(animeId);
        return PagedResponse.of(comments.findRootComments(animeId, Pages.of(page, size, "createdAt,desc", Set.of("createdAt"))).map(mapper::comment));
    }

    @Transactional(readOnly = true)
    @Override
    public PagedResponse<CommentResponse> replies(Long commentId, int page, int size) {
        var pageable = Pages.of(page, size, "createdAt,desc", Set.of("createdAt"));
        comments.findById(commentId).orElseThrow(ApiException::missing);
        return PagedResponse.of(comments.findReplies(commentId, pageable).map(mapper::comment));
    }

    @Override
    public CommentResponse comment(Long userId, Long animeId, CommentRequest request) {
        Comment c = new Comment();
        c.setUser(users.require(userId));
        c.setAnime(catalog.require(animeId));
        if (request.parentCommentId() != null) {
            if (request.parentCommentId() < 1)
                throw new ApiException(400, "Parent comment ID must be positive");
            var parent = comments.findByIdForUpdate(request.parentCommentId()).orElseThrow(ApiException::missing);
            if (parent.getDeletedAt() != null)
                throw ApiException.missing();
            if (!parent.getAnime().getId().equals(animeId))
                throw new ApiException(400, "Parent comment belongs to another anime");
            c.setParent(parent);
        }
        c.setBody(request.body());
        return mapper.comment(comments.save(c));
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        var c = comments.findByIdForUpdate(commentId).orElseThrow(ApiException::missing);
        if (!c.getUser().getId().equals(userId)) throw new ApiException(403, "Only the comment owner can delete it");
        if (c.getDeletedAt() == null) c.setDeletedAt(Instant.now());
    }
}
