package az.animeazerbaycan.common;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException missing() {
        return new ApiException(404, "Resource not found");
    }
}
