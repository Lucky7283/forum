package az.animeazerbaycan;
import az.animeazerbaycan.security.JwtService;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;
class JwtServiceTest {
 @Test void signsAndChecksSignature() {
  JwtService service=new JwtService("test-key-with-at-least-thirty-two-bytes",Duration.ofHours(1));
  String token=service.issue(42L); assertThat(service.userId(token)).isEqualTo(42L);
  JwtService other=new JwtService("different-key-with-at-least-thirty-two-bytes",Duration.ofHours(1));
  assertThatThrownBy(()->other.userId(token)).isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
 }
 @Test void rejectsUnsafeConfiguration() {
  assertThatThrownBy(()->new JwtService("short",Duration.ofHours(1))).isInstanceOf(IllegalArgumentException.class);
 }
}
