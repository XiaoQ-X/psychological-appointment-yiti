package cn.schoolpsych.appointment.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final int accessTokenMinutes;

    public TokenService(
            ObjectMapper objectMapper,
            @Value("${app.security.token-secret}") String tokenSecret,
            @Value("${app.security.access-token-minutes}") int accessTokenMinutes) {
        if (tokenSecret == null || tokenSecret.length() < 32) {
            throw new IllegalStateException("app.security.token-secret must be at least 32 characters");
        }
        this.objectMapper = objectMapper;
        this.secret = tokenSecret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String createAccessToken(Account account) {
        long expiresAt = Instant.now().plusSeconds(accessTokenMinutes * 60L).getEpochSecond();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "sub", account.getId(),
                "username", account.getUsername(),
                "role", account.getRole().name(),
                "exp", expiresAt,
                "pwd", account.passwordVersion());
        String encodedHeader = encode(header);
        String encodedPayload = encode(payload);
        String signedContent = encodedHeader + "." + encodedPayload;
        return signedContent + "." + sign(signedContent);
    }

    public TokenClaims parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid token format");
            }
            String signedContent = parts[0] + "." + parts[1];
            String expectedSignature = sign(signedContent);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new IllegalArgumentException("Invalid token signature");
            }
            Map<String, Object> header = decode(parts[0]);
            if (!"HS256".equals(header.get("alg"))) {
                throw new IllegalArgumentException("Unsupported token algorithm");
            }
            Map<String, Object> payload = decode(parts[1]);
            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (expiresAt <= Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("Token expired");
            }
            return new TokenClaims(
                    ((Number) payload.get("sub")).longValue(),
                    (String) payload.get("username"),
                    AccountRole.valueOf((String) payload.get("role")),
                    expiresAt,
                    (String) payload.get("pwd"));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid token", exception);
        }
    }

    private String encode(Map<String, Object> content) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(content));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to encode token", exception);
        }
    }

    private Map<String, Object> decode(String content) {
        try {
            byte[] bytes = URL_DECODER.decode(content);
            return objectMapper.readValue(bytes, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to decode token", exception);
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to sign token", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }
}
