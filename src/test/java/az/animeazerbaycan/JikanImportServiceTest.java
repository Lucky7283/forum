package az.animeazerbaycan;

import az.animeazerbaycan.animeimporter.Implementation.JikanImportService;
import az.animeazerbaycan.dto.jikan.JikanAnime;
import az.animeazerbaycan.dto.jikan.JikanLink;
import az.animeazerbaycan.animeimporter.*;
import az.animeazerbaycan.entity.*;
import az.animeazerbaycan.repository.*;
import az.animeazerbaycan.mapper.JikanMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class JikanImportServiceTest {
    private final JikanClient client = mock(JikanClient.class);
    private final AnimeRepository originals = mock(AnimeRepository.class);
    private final AnimeTranslatedRepository translated = mock(AnimeTranslatedRepository.class);
    private final az.animeazerbaycan.service.Interface.AnimeGenreService genres = mock(az.animeazerbaycan.service.Interface.AnimeGenreService.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);

    private JikanImportService service() {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new JikanImportService(client, originals, translated, genres, new JikanMapper(), new TransactionTemplate(manager));
    }

    @Test
    void outageDoesNotWriteToDatabase() {
        when(client.fetch(1)).thenThrow(new IllegalStateException("offline"));
        assertThatThrownBy(() -> service().importAnime(1)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(originals, translated, genres);
    }

    @Test
    void importsWithoutOverwritingTranslationOrDuplicatingLinks() {
        var link = new JikanLink("Official", "https://example.com");
        when(client.fetch(1)).thenReturn(new JikanAnime(1, "Original", null, null, null, null, List.of(), null, null, null, null, List.of(link), List.of(link)));
        Anime original = new Anime();
        original.setId(5L);
        original.setSlug("stable");
        when(originals.findByMalId(1)).thenReturn(Optional.of(original));
        AnimeTranslated local = new AnimeTranslated();
        local.setTitle("Tercume");
        local.setSynopsis("Local synopsis");
        when(translated.findByMalId(1)).thenReturn(Optional.of(local));
        var service = service();
        assertThat(service.importAnime(1)).isEqualTo(5L);
        service.importAnime(1);
        assertThat(original.getTitle()).isEqualTo("Original");
        assertThat(original.getSlug()).isEqualTo("stable");
        assertThat(local.getTitle()).isEqualTo("Tercume");
        assertThat(local.getSynopsis()).isEqualTo("Local synopsis");
        assertThat(local.getExternalLinks()).hasSize(1);
    }
}
