package az.animeazerbaycan.common;

import org.springframework.data.domain.*;

import java.util.Set;

public final class Pages {
    private Pages() {
    }

    public static Pageable of(int page, int size, String sort, Set<String> allowed) {
        if (page < 0 || size < 1 || size > 100)
            throw new ApiException(400, "Page must be nonnegative and size between 1 and 100");
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !allowed.contains(parts[0]) || !(parts[1].equals("asc") || parts[1].equals("desc")))
            throw new ApiException(400, "Unsupported sort");
        return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(parts[1]), parts[0]).and(Sort.by("id")));
    }
}
