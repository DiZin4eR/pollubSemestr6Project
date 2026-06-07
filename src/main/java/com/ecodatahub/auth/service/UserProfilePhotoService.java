package com.ecodatahub.auth.service;

import com.ecodatahub.auth.domain.UserAccount;
import com.ecodatahub.auth.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfilePhotoService {

    private static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            MediaType.IMAGE_JPEG_VALUE, "jpg",
            MediaType.IMAGE_PNG_VALUE, "png",
            "image/webp", "webp"
    );

    private final UserAccountRepository userAccountRepository;

    @Value("${app.upload.profile-photos-dir:uploads/profile-photos}")
    private String profilePhotosDir;

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void save(String username, MultipartFile file) {
        validate(file);

        UserAccount account = getAccount(username);
        String contentType = file.getContentType();
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        String filename = "user-" + account.getId() + "." + extension;
        Path uploadPath = uploadPath();
        Path targetPath = uploadPath.resolve(filename).normalize();
        String previousPath = account.getProfilePhotoPath();

        try {
            Files.createDirectories(uploadPath);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            if (!targetPath.toString().equals(previousPath)) {
                deleteStoredFile(previousPath);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store profile photo", exception);
        }

        account.setProfilePhotoPath(targetPath.toString());
        account.setProfilePhotoContentType(contentType);
        userAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Optional<ProfilePhoto> load(String username) {
        UserAccount account = getAccount(username);

        if (account.getProfilePhotoPath() == null || account.getProfilePhotoContentType() == null) {
            return Optional.empty();
        }

        Path path = Path.of(account.getProfilePhotoPath()).normalize();

        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }

        return Optional.of(new ProfilePhoto(new FileSystemResource(path), account.getProfilePhotoContentType()));
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void delete(String username) {
        UserAccount account = getAccount(username);
        deleteStoredFile(account.getProfilePhotoPath());

        account.setProfilePhotoPath(null);
        account.setProfilePhotoContentType(null);
        userAccountRepository.save(account);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Select a profile photo to upload.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Profile photo must be 2 MB or smaller.");
        }

        if (!EXTENSIONS_BY_CONTENT_TYPE.containsKey(file.getContentType())) {
            throw new IllegalArgumentException("Profile photo must be JPEG, PNG, or WebP.");
        }
    }

    private UserAccount getAccount(String username) {
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user is not stored"));
    }

    private Path uploadPath() {
        return Path.of(profilePhotosDir).toAbsolutePath().normalize();
    }

    private void deleteStoredFile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(Path.of(storedPath).normalize());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete old profile photo", exception);
        }
    }

    public record ProfilePhoto(
            Resource resource,
            String contentType
    ) {
    }
}
