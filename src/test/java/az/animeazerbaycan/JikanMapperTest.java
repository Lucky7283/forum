package az.animeazerbaycan;

import az.animeazerbaycan.dto.jikan.JikanAired;
import az.animeazerbaycan.dto.jikan.JikanAnime;
import az.animeazerbaycan.dto.jikan.JikanImage;
import az.animeazerbaycan.dto.jikan.JikanImages;
import az.animeazerbaycan.dto.jikan.JikanReference;
import az.animeazerbaycan.entity.Anime;
import az.animeazerbaycan.mapper.JikanMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
class JikanMapperTest {
 private final JikanMapper mapper=new JikanMapper();
 @Test void mapsMetadataAndPreservesStableSlug() {
  var source=new JikanAnime(1,"Cowboy Bebop","Synopsis",new JikanImages(new JikanImage("https://example.com/image.jpg")),new JikanAired("1998-04-03T00:00:00+09:00",null),new BigDecimal("8.75"),List.of(),List.of(new JikanReference(1,"Sunrise")),"TV","Finished Airing",26,List.of(),List.of());
  Anime target=new Anime(); mapper.apply(source,target);
  assertThat(target.getSlug()).isEqualTo("cowboy-bebop-1"); assertThat(target.getStartDate()).hasToString("1998-04-03"); assertThat(target.getAiringStatus()).isEqualTo("finished_airing"); assertThat(target.getStudio()).isEqualTo("Sunrise"); assertThat(target.getMalMean()).isEqualByComparingTo("8.75");
  target.setSlug("previous-slug"); mapper.apply(source,target); assertThat(target.getSlug()).isEqualTo("previous-slug");
 }
 @Test void handlesMissingOptionalMetadata() {
  Anime target=new Anime(); mapper.apply(new JikanAnime(2,"Title",null,null,null,null,null,null,null,null,null,null,null),target);
  assertThat(target.getStartDate()).isNull(); assertThat(target.getPictureUrl()).isNull(); assertThat(target.getStudio()).isNull();
 }
}
