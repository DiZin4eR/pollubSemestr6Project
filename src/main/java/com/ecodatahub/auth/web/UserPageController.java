package com.ecodatahub.auth.web;

import com.ecodatahub.auth.service.UserAccountService;
import com.ecodatahub.auth.service.UserProfilePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class UserPageController {

    private final UserAccountService userAccountService;
    private final UserProfilePhotoService userProfilePhotoService;

    @GetMapping("/users")
    public String users(Authentication authentication, Model model) {
        model.addAttribute("user", userAccountService.getUser(authentication.getName()));

        return "users";
    }

    @PostMapping(value = "/users/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadProfilePhoto(
            @RequestParam("file") MultipartFile file,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userProfilePhotoService.save(authentication.getName(), file);
            redirectAttributes.addFlashAttribute("profileMessage", "Profile photo updated.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("profileError", exception.getMessage());
        }

        return "redirect:/users";
    }

    @GetMapping("/users/profile-photo")
    @ResponseBody
    public ResponseEntity<Resource> profilePhoto(Authentication authentication) {
        return userProfilePhotoService.load(authentication.getName())
                .map(photo -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(photo.contentType()))
                        .cacheControl(CacheControl.noCache())
                        .body(photo.resource()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/users/profile-photo/delete")
    public String deleteProfilePhoto(
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userProfilePhotoService.delete(authentication.getName());
            redirectAttributes.addFlashAttribute("profileMessage", "Profile photo removed.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("profileError", exception.getMessage());
        }

        return "redirect:/users";
    }
}
