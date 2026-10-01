package az.animeazerbaycan;
import az.animeazerbaycan.entity.*;
import az.animeazerbaycan.service.Interface.*;
import az.animeazerbaycan.service.Implementation.CommunityServiceImpl;
import az.animeazerbaycan.repository.*;
import az.animeazerbaycan.mapper.ResponseMapper;
import az.animeazerbaycan.common.ApiException;
import az.animeazerbaycan.service.Interface.CatalogService;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class CommunityServiceTest {
 private final CommentRepository comments=mock(CommentRepository.class);
 private final CommunityService service=new CommunityServiceImpl(mock(UserService.class),mock(CatalogService.class),mock(RatingRepository.class),mock(FavoriteRepository.class),mock(WatchlistEntryRepository.class),comments,new ResponseMapper());
 @Test void onlyOwnerCanSoftDelete() {
  User owner=new User(); owner.setId(1L); Comment c=new Comment(); c.setUser(owner); when(comments.findByIdForUpdate(7L)).thenReturn(Optional.of(c));
  assertThatThrownBy(()->service.deleteComment(2L,7L)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.getStatus()).isEqualTo(403)); assertThat(c.getDeletedAt()).isNull();
  service.deleteComment(1L,7L); assertThat(c.getDeletedAt()).isNotNull(); var deletedAt=c.getDeletedAt(); service.deleteComment(1L,7L); assertThat(c.getDeletedAt()).isEqualTo(deletedAt); verify(comments,never()).delete(any());
 }
}
