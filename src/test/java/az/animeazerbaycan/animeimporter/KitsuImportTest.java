package az.animeazerbaycan.animeimporter;

import az.animeazerbaycan.animeimporter.Implementation.KitsuImportService;
import az.animeazerbaycan.dto.importer.ImportResult;
import az.animeazerbaycan.dto.importer.PageImportResult;
import az.animeazerbaycan.dto.kitsu.KitsuAnime;
import az.animeazerbaycan.dto.kitsu.KitsuAttributes;
import az.animeazerbaycan.dto.kitsu.KitsuImage;
import az.animeazerbaycan.dto.kitsu.KitsuLinks;
import az.animeazerbaycan.dto.kitsu.KitsuPageResponse;
import az.animeazerbaycan.entity.Anime;
import az.animeazerbaycan.entity.Enum.TranslateStatus;
import az.animeazerbaycan.mapper.KitsuMapper;
import az.animeazerbaycan.repository.AnimeRepository;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class KitsuImportTest {
    private final KitsuClient client = mock(KitsuClient.class);
    private final AnimeRepository originals = mock(AnimeRepository.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);

    private KitsuImportService service() {
        when(manager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        return new KitsuImportService(client, originals, new KitsuMapper(), new TransactionTemplate(manager), mock(az.animeazerbaycan.service.Interface.AnimeGenreService.class));
    }

    private KitsuAnime sample() throws Exception {
        try (var stream = getClass().getResourceAsStream("/kitsu/anime-page.json")) {
            return JsonMapper.builder().build().readValue(stream, KitsuPageResponse.class).data().getFirst();
        }
    }

    @Test void mapsActualSampleWithVerifiedMalIdentityAndPreservesExistingMetadata() throws Exception {
        var anime = new Anime();
        anime.setSlug("stable");
        anime.setMalMean(new BigDecimal("8.50"));
        anime.setStudio("Wit Studio");
        anime.setTranslationStatus(TranslateStatus.TRANSLATED);
        new KitsuMapper().apply(sample(), 16498, anime);
        assertThat(anime.getMalId()).isEqualTo(16498);
        assertThat(anime.getTitle()).isEqualTo("Attack on Titan");
        assertThat(anime.getStartDate()).hasToString("2013-04-07");
        assertThat(anime.getEndDate()).hasToString("2013-09-29");
        assertThat(anime.getNumEpisodes()).isEqualTo(25);
        assertThat(anime.getMediaType()).isEqualTo("tv");
        assertThat(anime.getAiringStatus()).isEqualTo("finished_airing");
        assertThat(anime.getPictureUrl()).contains("/7442/original.jpg");
        assertThat(anime.getSlug()).isEqualTo("stable");
        assertThat(anime.getMalMean()).isEqualByComparingTo("8.50");
        assertThat(anime.getStudio()).isEqualTo("Wit Studio");
        assertThat(anime.getTranslationStatus()).isEqualTo(TranslateStatus.TRANSLATED);
    }

    @Test void handlesNullableAttributesAndTitleImageFallbacks() {
        var data = new KitsuAttributes(null, java.util.Map.of("en_jp", "Test Title"), null,
                "Description", new KitsuImage(null,"large",null,null,null), null,null,"ONA","tba",null);
        var anime = new Anime();
        new KitsuMapper().apply(new KitsuAnime("7","anime",data), 42, anime);
        assertThat(anime.getTitle()).isEqualTo("Test Title");
        assertThat(anime.getSlug()).isEqualTo("test-title-42");
        assertThat(anime.getSynopsis()).isEqualTo("Description");
        assertThat(anime.getPictureUrl()).isEqualTo("large");
        assertThat(anime.getStartDate()).isNull();
        assertThat(anime.getAiringStatus()).isEqualTo("not_yet_aired");
        assertThat(anime.getMalMean()).isNull();
    }

    @Test void importsPagesByMalIdAndRerunsUpdateTheSameOriginal() throws Exception {
        var page = new KitsuPageResponse(List.of(sample()), new KitsuLinks(null));
        when(client.fetchPage(3)).thenReturn(page);
        when(client.findMalId("7442")).thenReturn(16498);
        var original = new Anime(); original.setId(4L); original.setSlug("stable");
        when(originals.findByMalId(16498)).thenReturn(Optional.of(original));
        var service = service();
        assertThat(service.importPages(3)).isEqualTo(new PageImportResult(1,3));
        assertThat(service.importPages(3)).isEqualTo(new PageImportResult(1,3));
        verify(originals,times(2)).saveAndFlush(original);
        verify(originals,never()).findByMalId(7442);
        verify(manager,times(2)).commit(any());
        verify(client,never()).fetchPage(4);
    }

    @Test void skipsUnmappedAnimeAndAdvancesToTheFinalPage() throws Exception {
        var first = new KitsuPageResponse(List.of(sample()), new KitsuLinks("next"));
        var last = new KitsuPageResponse(List.of(), new KitsuLinks(null));
        when(client.fetchPage(1)).thenReturn(first);
        when(client.findMalId("7442")).thenReturn(null);
        when(client.hasNext(first)).thenReturn(true);
        when(client.fetchPage(2)).thenReturn(last);
        assertThat(service().importPages(1)).isEqualTo(new PageImportResult(0,2));
        verifyNoInteractions(originals);
    }

    @Test void databaseFailureRollsBackAndIdentifiesResumePage() throws Exception {
        when(client.fetchPage(4)).thenReturn(new KitsuPageResponse(List.of(sample()), null));
        when(client.findMalId("7442")).thenReturn(16498);
        when(originals.saveAndFlush(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(() -> service().importPages(4)).hasMessageContaining("start-page=4");
        verify(manager).rollback(any());
        verify(client,never()).fetchPage(5);
    }

    @Test void singleAndRangeMethodsUseMalIds() throws Exception {
        when(client.fetchByMalId(16498)).thenReturn(sample());
        assertThat(service().importRange(16498,16499)).isEqualTo(new ImportResult(1,List.of(16499)));
        verify(originals).findByMalId(16498);
        verify(originals,never()).findByMalId(7442);
    }

    @Test void rejectsInvalidPagesAndHonorsInterruptionBeforeFetching() {
        var service = service();
        assertThatThrownBy(() -> service.importPages(0)).isInstanceOf(IllegalArgumentException.class);
        Thread.currentThread().interrupt();
        try { assertThatThrownBy(() -> service.importPages(1)).hasMessageContaining("interrupted"); }
        finally { Thread.interrupted(); }
        verifyNoInteractions(client, originals);
    }


}
