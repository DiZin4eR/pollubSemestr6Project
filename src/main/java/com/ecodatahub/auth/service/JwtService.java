package com.ecodatahub.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret:change-this-development-secret-change-me}") String secret,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(String username, String role) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", username);
            payload.put("role", role);
            payload.put("iat", Instant.now().getEpochSecond());
            payload.put("exp", Instant.now().plusSeconds(expirationSeconds).getEpochSecond());

            String headerPart = encodeJson(header);
            String payloadPart = encodeJson(payload);
            String signedPart = headerPart + "." + payloadPart;

            return signedPart + "." + sign(signedPart);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create JWT", exception);
        }
    }

    public Optional<String> getUsername(String token) {
        return getClaims(token).map(claims -> (String) claims.get("sub"));
    }

    public boolean isValid(String token) {
        return getClaims(token).isPresent();
    }

    private Optional<Map<String, Object>> getClaims(String token) {
        try {
            String[] parts = token.split("\\.");

            if (parts.length != 3) {
                return Optional.empty();
            }

            String signedPart = parts[0] + "." + parts[1];

            if (!constantTimeEquals(sign(signedPart), parts[2])) {
                return Optional.empty();
            }

            Map<String, Object> claims = objectMapper.readValue(
                    base64Decode(parts[1]),
                    new TypeReference<>() {
                    }
            );
            Number expiresAt = (Number) claims.get("exp");

            if (expiresAt == null || Instant.now().getEpochSecond() >= expiresAt.longValue()) {
                return Optional.empty();
            }

            return Optional.of(claims);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return base64Encode(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
        return base64Encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] base64Decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private boolean constantTimeEquals(String first, String second) {
        return MessageDigestHolder.equals(first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
    }

    private static class MessageDigestHolder {
        private static boolean equals(byte[] first, byte[] second) {
            return java.security.MessageDigest.isEqual(first, second);
        }
    }
}
