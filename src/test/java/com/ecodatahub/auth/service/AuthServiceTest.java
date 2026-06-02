package com.ecodatahub.auth.service;

import com.ecodatahub.auth.domain.UserAccount;
import com.ecodatahub.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void signupTrimsUsernameHashesPasswordStoresUserRoleAndReturnsToken() {
        when(userAccountRepository.existsByUsername("  new-user  ")).thenReturn(false);
        when(passwordEncoder.encode("secret1")).thenReturn("encoded-password");
        when(jwtService.createToken("new-user", "USER")).thenReturn("jwt-token");

        String token = authService.signup("  new-user  ", "secret1");

        assertThat(token).isEqualTo("jwt-token");
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("new-user");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(captor.getValue().getRole()).isEqualTo("USER");
    }

    @Test
    void signupRejectsDuplicateUsername() {
        when(userAccountRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup("admin", "secret1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already taken");

        verify(userAccountRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void signupRejectsWeakCredentialsBeforeSaving() {
        assertThatThrownBy(() -> authService.signup("ab", "secret1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username must have at least 3 characters");

        assertThatThrownBy(() -> authService.signup("admin", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must have at least 6 characters");

        verify(userAccountRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginReturnsTokenWhenPasswordMatches() {
        UserAccount account = userAccount("admin", "hash", "ADMIN");
        when(userAccountRepository.findByUsername("admin")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("secret1", "hash")).thenReturn(true);
        when(jwtService.createToken("admin", "ADMIN")).thenReturn("jwt-token");

        String token = authService.login("admin", "secret1");

        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void loginRejectsUnknownUserOrWrongPassword() {
        when(userAccountRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("missing", "secret1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");

        UserAccount account = userAccount("admin", "hash", "ADMIN");
        when(userAccountRepository.findByUsername("admin")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("bad-password", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("admin", "bad-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid username or password");
    }

    private UserAccount userAccount(String username, String passwordHash, String role) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordHash);
        account.setRole(role);
        return account;
    }
}
