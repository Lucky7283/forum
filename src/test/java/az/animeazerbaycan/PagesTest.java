package az.animeazerbaycan;
import az.animeazerbaycan.common.Pages;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
class PagesTest {
 @Test void rejectsInvalidPaginationAndUnlistedSorts() {
  for(int size:new int[]{0,-1,101}) assertThatThrownBy(()->Pages.of(0,size,"title,asc",Set.of("title"))).isInstanceOf(RuntimeException.class);
  assertThatThrownBy(()->Pages.of(-1,20,"title,asc",Set.of("title"))).isInstanceOf(RuntimeException.class);
  for(String sort:new String[]{"password,asc","title","title,invalid","title,asc,extra"}) assertThatThrownBy(()->Pages.of(0,20,sort,Set.of("title"))).isInstanceOf(RuntimeException.class);
  assertThat(Pages.of(0,20,"title,asc",Set.of("title")).getSort().getOrderFor("id")).isNotNull();
 }
}
