package az.animeazerbaycan.animetranslation;

import az.animeazerbaycan.service.Interface.AnimeTranslationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@ConditionalOnProperty(name = "app.anime-translation.enabled", havingValue = "true")
@RequiredArgsConstructor
public class TranslationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TranslationRunner.class);
    private final AnimeTranslationService service;

    @Override
    public void run(ApplicationArguments args) {
        try {
            service.translateAnime();
            log.info("Translation batch finished");
        } catch (RuntimeException failure) {
            log.error("Translation stopped; application remains available: {}", failure.getMessage(), failure);
        }
    }
}
