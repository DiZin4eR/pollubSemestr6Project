package com.ecodatahub.auth.service;

import com.ecodatahub.auth.domain.UserAccount;
import com.ecodatahub.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public String signup(String username, String password) {
        validateCredentials(username, password);

        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken");
        }

        UserAccount account = new UserAccount();
        account.setUsername(username.trim());
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole("USER");

        userAccountRepository.save(account);

        return jwtService.createToken(account.getUsername(), account.getRole());
    }

    @Transactional(readOnly = true)
    public String login(String username, String password) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        return jwtService.createToken(account.getUsername(), account.getRole());
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.isBlank() || username.length() < 3) {
            throw new IllegalArgumentException("Username must have at least 3 characters");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must have at least 6 characters");
        }
    }
}
