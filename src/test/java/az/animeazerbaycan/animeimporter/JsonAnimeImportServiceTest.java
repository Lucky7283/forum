package az.animeazerbaycan.animeimporter;

import az.animeazerbaycan.animeimporter.Implementation.JsonAnimeImportService;
import az.animeazerbaycan.dto.importer.FileImportResult;
import az.animeazerbaycan.entity.Anime;
import az.animeazerbaycan.mapper.Anime5kMapper;
import az.animeazerbaycan.repository.AnimeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JsonAnimeImportServiceTest {
    @TempDir Path directory;
    private final AnimeRepository originals = mock(AnimeRepository.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private static final String ENTRY = """
      {"meta-data":{"id":1,"title":"Cowboy Bebop","type":"TV","score":8.75,"studio":"Sunrise"},
       "data":{"synopsis":"Description","aired":{"from":"1998-04","to":"1999-04"}}}
      """;
    private JsonAnimeImportService service(String location) {
        when(manager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        return new JsonAnimeImportService(originals, new Anime5kMapper(), new TransactionTemplate(manager), mock(az.animeazerbaycan.service.Interface.AnimeGenreService.class), location);
    }
    @Test void jsonlPreservesExistingFieldsAndSkipsDuplicate() throws Exception {
        Path file = directory.resolve("anime.jsonl"); Files.writeString(file, ENTRY + ENTRY);
        var original = new Anime(); original.setId(9L); original.setSlug("stable");
        original.setPictureUrl("https://example.com/cover.jpg"); original.setStartDate(LocalDate.of(1998,4,3));
        when(originals.findByMalId(1)).thenReturn(Optional.of(original));
        assertThat(service(file.toString()).importFile(file.toString()))
            .isEqualTo(new FileImportResult(2,1,0,1));
        assertThat(original.getTitle()).isEqualTo("Cowboy Bebop");
        assertThat(original.getSlug()).isEqualTo("stable");
        assertThat(original.getPictureUrl()).isEqualTo("https://example.com/cover.jpg");
        assertThat(original.getStartDate()).isEqualTo(LocalDate.of(1998,4,3));
        assertThat(original.getMalMean()).isEqualByComparingTo("8.75");
        verify(manager).commit(any());
    }
    @Test void arraySkipsInvalidEntry() throws Exception {
        Path file = directory.resolve("anime.json"); Files.writeString(file,"[{}," + ENTRY + "]");
        assertThat(service(file.toString()).importFile(file.toString()))
            .isEqualTo(new FileImportResult(2,1,1,0));
        verify(originals).saveAndFlush(any());
    }
    @Test void classpathResourceImportsWithoutFileSystemPath() {
        String location = "classpath:fixtures/anime5k.jsonl";
        assertThat(service(location).importFile(location).imported()).isEqualTo(1);
        verify(originals).saveAndFlush(any());
    }
    @Test void databaseFailureRollsBackAndStops() throws Exception {
        Path file = directory.resolve("anime.json"); Files.writeString(file,ENTRY);
        when(originals.saveAndFlush(any())).thenThrow(new IllegalStateException("database offline"));
        assertThatThrownBy(() -> service(file.toString()).importFile(file.toString())).hasMessageContaining("after saving 0");
        verify(manager).rollback(any());
    }
    @Test void malformedJsonReportsCommittedProgress() throws Exception {
        Path file = directory.resolve("anime.jsonl"); Files.writeString(file,ENTRY + "{broken");
        assertThatThrownBy(() -> service(file.toString()).importFile(file.toString())).hasMessageContaining("after saving 1");
        verify(manager).commit(any());
    }
}
