package com.ecodatahub.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createTokenReturnsValidTokenForOriginalSubject() {
        JwtService jwtService = new JwtService(objectMapper, "test-secret", 60);

        String token = jwtService.createToken("admin", "ADMIN");

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.getUsername(token)).contains("admin");
    }

    @Test
    void isValidRejectsTamperedToken() {
        JwtService jwtService = new JwtService(objectMapper, "test-secret", 60);
        String token = jwtService.createToken("admin", "ADMIN");
        String tamperedToken = token.substring(0, token.length() - 1) + "x";

        assertThat(jwtService.isValid(tamperedToken)).isFalse();
        assertThat(jwtService.getUsername(tamperedToken)).isEmpty();
    }

    @Test
    void isValidRejectsExpiredToken() {
        JwtService jwtService = new JwtService(objectMapper, "test-secret", -1);

        String token = jwtService.createToken("admin", "ADMIN");

        assertThat(jwtService.isValid(token)).isFalse();
        assertThat(jwtService.getUsername(token)).isEmpty();
    }
}
