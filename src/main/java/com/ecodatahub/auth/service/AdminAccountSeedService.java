package com.ecodatahub.auth.service;

import com.ecodatahub.auth.domain.UserAccount;
import com.ecodatahub.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminAccountSeedService implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.username:admin}")
    private String adminUsername;

    @Value("${app.security.admin.password:admin}")
    private String adminPassword;

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void run(ApplicationArguments args) {
        if (userAccountRepository.existsByUsername(adminUsername)) {
            return;
        }

        UserAccount account = new UserAccount();
        account.setUsername(adminUsername);
        account.setPasswordHash(passwordEncoder.encode(adminPassword));
        account.setRole("ADMIN");

        userAccountRepository.save(account);
    }
}
