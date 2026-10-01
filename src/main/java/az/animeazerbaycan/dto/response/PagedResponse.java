package az.animeazerbaycan.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PagedResponse<T>(List<T> data, MetaResponse meta) {
    public static <T> PagedResponse<T> of(Page<T> p) {
        return new PagedResponse<>(p.getContent(), new MetaResponse(p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages()));
    }
}
