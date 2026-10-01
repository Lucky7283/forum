package az.animeazerbaycan.security;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.*;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration ttl;

    public JwtService(@Value("${app.jwt.signing-key}") String secret, @Value("${app.jwt.ttl}") Duration ttl) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32 || ttl.isNegative() || ttl.isZero())
            throw new IllegalArgumentException("JWT configuration is invalid");
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
        var d = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        d.setJwtValidator(JwtValidators.createDefaultWithIssuer("anime-azerbaycan"));
        decoder = d;
        this.ttl = ttl;
    }

    public String issue(Long userId) {
        Instant now = Instant.now();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("anime-azerbaycan").subject(userId.toString()).issuedAt(now)
                        .expiresAt(now.plus(ttl)).build()))
                .getTokenValue();
    }

    public Long userId(String token) {
        return Long.valueOf(decoder.decode(token).getSubject());
    }

    public Duration ttl() {
        return ttl;
    }
}
