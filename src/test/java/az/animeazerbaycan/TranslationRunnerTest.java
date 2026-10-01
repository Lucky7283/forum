package az.animeazerbaycan;

import az.animeazerbaycan.animetranslation.TranslationRunner;
import az.animeazerbaycan.animeimporter.ImportRunner;
import az.animeazerbaycan.service.Interface.AnimeTranslationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.annotation.Order;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TranslationRunnerTest {
    private final AnimeTranslationService service = mock(AnimeTranslationService.class);
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(TranslationRunner.class)
            .withBean(AnimeTranslationService.class, () -> service);

    @Test void absentOrFalseFlagDoesNotRegisterRunner() {
        context.run(ctx -> assertThat(ctx).doesNotHaveBean(TranslationRunner.class));
        context.withPropertyValues("app.anime-translation.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(TranslationRunner.class));
        verifyNoInteractions(service);
    }

    @Test void enabledRunnerCallsOneConfiguredBatchAfterImport() {
        context.withPropertyValues("app.anime-translation.enabled=true").run(ctx -> {
            ctx.getBean(TranslationRunner.class).run(null);
            verify(service).translateAnime();
            verifyNoMoreInteractions(service);
        });
        assertThat(TranslationRunner.class.getAnnotation(Order.class).value())
                .isGreaterThan(ImportRunner.class.getAnnotation(Order.class).value());
    }

    @Test void translationFailureDoesNotStopTheApplication() {
        doThrow(new IllegalStateException("test translation outage")).when(service).translateAnime();
        assertThatCode(() -> new TranslationRunner(service).run(null)).doesNotThrowAnyException();
        verify(service).translateAnime();
    }
}
