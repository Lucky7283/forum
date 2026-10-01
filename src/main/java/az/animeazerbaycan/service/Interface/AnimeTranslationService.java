package az.animeazerbaycan.service.Interface;

/** Manually invoked English-to-Azerbaijani synopsis translation. */
public interface AnimeTranslationService {
    /** Translates up to the configured batch size; already translated anime are skipped. */
    void translateAnime();

    /**
     * Translates at most {@code limit} pending anime and returns the number saved.
     * Each anime commits separately. A failure stops the batch; rerunning skips saved records.
     */
    int translateAnime(int limit);
}
