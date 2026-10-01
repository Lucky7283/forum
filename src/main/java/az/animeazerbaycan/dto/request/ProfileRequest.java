package az.animeazerbaycan.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ProfileRequest {
    @Pattern(regexp = "[\\p{L}0-9_]{3,50}")
    private String username;
    @Size(max = 2048)
    @Pattern(regexp = "https?://[^\\s]+")
    private String avatarUrl;
    private boolean avatarProvided;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    @com.fasterxml.jackson.annotation.JsonSetter("avatarUrl")
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        avatarProvided = true;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isAvatarProvided() {
        return avatarProvided;
    }
}
