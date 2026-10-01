package az.animeazerbaycan.mapper;

import az.animeazerbaycan.dto.response.AnimeResponse;
import az.animeazerbaycan.dto.response.CommentResponse;
import az.animeazerbaycan.dto.response.GenreResponse;
import az.animeazerbaycan.dto.response.LinkResponse;
import az.animeazerbaycan.dto.response.ProfileResponse;
import az.animeazerbaycan.dto.response.UserResponse;
import org.springframework.stereotype.Component;
import java.util.List;
import az.animeazerbaycan.entity.*;

@Component
public class ResponseMapper {
    public UserResponse user(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getAvatarUrl(), u.getRole(), u.getCreatedAt());
    }

    public ProfileResponse profile(User u) {
        return new ProfileResponse(u.getId(), u.getUsername(), u.getAvatarUrl(), u.getCreatedAt());
    }

    public GenreResponse genre(Genre g) {
        return new GenreResponse(g.getId(), g.getMalId(), g.getName(), g.getSlug());
    }

    public AnimeResponse anime(AnimeTranslated a, String slug, Double average, long count, List<Genre> genres) {
        return new AnimeResponse(a.getId(), a.getMalId(), slug, a.getTitle(), a.getSynopsis(), a.getPictureUrl(), a.getStudio(), a.getStartDate(), a.getEndDate(), a.getMalMean(), a.getMediaType(), a.getAiringStatus(), a.getNumEpisodes(), a.getCreatedAt(), a.getUpdatedAt(), genres.stream().sorted(java.util.Comparator.comparing(Genre::getName)).map(this::genre).toList(), a.getExternalLinks().stream().map(l -> new LinkResponse(l.getId(), l.getLabel(), l.getUrl())).toList(), average, count);
    }

    public CommentResponse comment(Comment c) {
        return new CommentResponse(c.getId(), c.getAnime().getId(), profile(c.getUser()),
                c.getDeletedAt() == null ? c.getBody() : null, c.getCreatedAt(),
                c.getParent() == null ? null : c.getParent().getId(), c.getDeletedAt() != null);
    }
}
