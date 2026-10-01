package az.animeazerbaycan;

import az.animeazerbaycan.dto.request.ProfileRequest;
import az.animeazerbaycan.dto.request.RegisterRequest;
import az.animeazerbaycan.dto.response.MetaResponse;
import az.animeazerbaycan.dto.response.PagedResponse;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DtoContractTest {
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void profileUpdateDistinguishesOmittedAvatarFromExplicitNull() {
        var omitted = json.readValue("{}", ProfileRequest.class);
        var cleared = json.readValue("{\"avatarUrl\":null}", ProfileRequest.class);
        var changed = json.readValue("{\"avatarUrl\":\"https://example.com/avatar.png\"}", ProfileRequest.class);

        assertThat(omitted.isAvatarProvided()).isFalse();
        assertThat(cleared.isAvatarProvided()).isTrue();
        assertThat(cleared.getAvatarUrl()).isNull();
        assertThat(changed.isAvatarProvided()).isTrue();
        assertThat(changed.getAvatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(json.writeValueAsString(changed)).doesNotContain("avatarProvided");
    }

    @Test
    void registrationRetainsValidationConstraints() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new RegisterRequest("valid_user", "user@example.com", "password123"))).isEmpty();
            assertThat(validator.validate(new RegisterRequest("!", "invalid", "short")))
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("username", "email", "password");
        }
    }

    @Test
    void pagedResponseRetainsJsonShape() {
        var response = new PagedResponse<>(List.of("anime"), new MetaResponse(0, 10, 1, 1));
        assertThat(json.readTree(json.writeValueAsString(response)))
                .isEqualTo(json.readTree("""
                        {"data":["anime"],"meta":{"page":0,"size":10,"totalElements":1,"totalPages":1}}
                        """));
    }
}
