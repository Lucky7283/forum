package az.animeazerbaycan.service.Interface;

import java.util.List;

public interface AnimeGenreService {
    /** Null means not supplied (preserve); an empty list means no genres. Call within the anime save transaction. */
    void replace(Integer animeMalId, List<String> names);
}
