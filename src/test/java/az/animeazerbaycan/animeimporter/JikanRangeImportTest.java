package az.animeazerbaycan.animeimporter;

import az.animeazerbaycan.animeimporter.Implementation.JikanImportService;
import az.animeazerbaycan.dto.importer.PageImportResult;
import az.animeazerbaycan.dto.jikan.JikanAnime;
import az.animeazerbaycan.dto.jikan.JikanPageResponse;
import az.animeazerbaycan.dto.jikan.JikanPagination;
import az.animeazerbaycan.entity.Anime;
import az.animeazerbaycan.mapper.JikanMapper;
import az.animeazerbaycan.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JikanRangeImportTest {
    private final JikanClient client = mock(JikanClient.class);
    private final AnimeRepository originals = mock(AnimeRepository.class);
    private final AnimeTranslatedRepository translated = mock(AnimeTranslatedRepository.class);
    private final az.animeazerbaycan.service.Interface.AnimeGenreService genres = mock(az.animeazerbaycan.service.Interface.AnimeGenreService.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);

    private JikanImportService service() {
        when(manager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        return new JikanImportService(client, originals, translated, genres, new JikanMapper(), new TransactionTemplate(manager));
    }

    private JikanAnime anime(int id) {
        return new JikanAnime(id,"Title "+id,null,null,null,null,List.of(),List.of(),null,null,null,List.of(),List.of());
    }

    @Test void skipsMissingIdAndCommitsOtherRecordsIncludingEndOfRange() {
        when(client.fetch(1)).thenReturn(anime(1));
        when(client.fetch(2)).thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND,"missing",HttpHeaders.EMPTY,new byte[0],null));
        when(client.fetch(3)).thenReturn(anime(3));
        var existing=new Anime(); existing.setId(4L); existing.setSlug("stable");
        when(originals.findByMalId(1)).thenReturn(Optional.of(existing));
        var result=service().importRange(1,3);
        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.skippedIds()).containsExactly(2);
        assertThat(existing.getSlug()).isEqualTo("stable");
        verify(manager,times(2)).commit(any());
        verify(originals,times(2)).saveAndFlush(any());
        verify(originals,never()).findByMalId(2);
        verify(client,never()).fetch(4);
    }

    @Test void retriesRateLimitBeforeSaving() {
        when(client.fetch(5)).thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS)).thenReturn(anime(5));
        assertThat(service().importRange(5,5).imported()).isEqualTo(1);
        verify(client,times(2)).fetch(5);
        verify(originals).saveAndFlush(any());
    }

    @Test void databaseFailureRollsBackAndStopsWithResumeId() {
        when(client.fetch(7)).thenReturn(anime(7));
        when(originals.saveAndFlush(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(()->service().importRange(7,8)).hasMessageContaining("from-id=7");
        verify(manager).rollback(any());
        verify(client,never()).fetch(8);
    }

    @Test void rejectsInvalidRangesBeforeAnyRequest() {
        var service=service();
        assertThatThrownBy(()->service.importRange(0,500)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.importRange(5,4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.importRange(1,501)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client, originals);
    }

    

    @Test void importsPagesIntoOriginalsOnlyAndPreservesSlug() {
        when(client.fetchPage(1)).thenReturn(new JikanPageResponse(List.of(anime(1)),new JikanPagination(true,1)));
        when(client.fetchPage(2)).thenReturn(new JikanPageResponse(List.of(anime(2)),new JikanPagination(false,2)));
        when(client.fetch(1)).thenReturn(anime(1));
        when(client.fetch(2)).thenReturn(anime(2));
        var original=new Anime(); original.setSlug("stable");
        when(originals.findByMalId(1)).thenReturn(Optional.of(original));
        assertThat(service().importPages(1)).isEqualTo(new PageImportResult(2,2));
        assertThat(original.getSlug()).isEqualTo("stable");
        assertThat(original.getTitle()).isEqualTo("Title 1");
        verify(originals,times(2)).saveAndFlush(any());
        verify(manager,times(2)).commit(any());
        verifyNoInteractions(translated);
        verify(genres).replace(eq(1), any());
        verify(genres).replace(eq(2), any());
        verify(client).fetch(1);
        verify(client).fetch(2);
        verify(client,never()).fetchPage(3);
    }

    @Test void pageDatabaseFailureStopsAndReportsResumePage() {
        when(client.fetch(1)).thenReturn(anime(1));
        when(client.fetchPage(4)).thenReturn(new JikanPageResponse(List.of(anime(1)),new JikanPagination(true,4)));
        when(originals.saveAndFlush(any())).thenThrow(new IllegalStateException("offline"));
        assertThatThrownBy(()->service().importPages(4)).hasMessageContaining("start-page=4");
        verify(manager).rollback(any());
        verify(client,never()).fetchPage(5);
    }

    @Test void pageRateLimitRetriesAndEmptyLastPageStops() {
        when(client.fetchPage(7)).thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))
            .thenReturn(new JikanPageResponse(List.of(),new JikanPagination(false,7)));
        assertThat(service().importPages(7).imported()).isZero();
        verify(client,times(2)).fetchPage(7);
        verifyNoInteractions(originals,translated,genres);
    }
}
