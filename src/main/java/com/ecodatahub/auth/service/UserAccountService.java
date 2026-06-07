package com.ecodatahub.auth.service;

import com.ecodatahub.auth.domain.UserAccount;
import com.ecodatahub.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    @Transactional(readOnly = true)
    public UserAccountSummary getUser(String username) {
        return userAccountRepository.findByUsername(username)
                .map(UserAccountSummary::from)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user is not stored"));
    }

    public record UserAccountSummary(
            Long id,
            String username,
            String role,
            boolean hasProfilePhoto
    ) {
        private static UserAccountSummary from(UserAccount account) {
            return new UserAccountSummary(
                    account.getId(),
                    account.getUsername(),
                    account.getRole(),
                    account.getProfilePhotoPath() != null
            );
        }
    }
}
