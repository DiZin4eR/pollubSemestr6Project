package com.ecodatahub.auth.web;

import com.ecodatahub.auth.service.UserAccountService;
import com.ecodatahub.auth.service.UserProfilePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
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
                .orElseGet(() -> fallbackAvatar(authentication.getName()));
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

    private ResponseEntity<Resource> fallbackAvatar(String username) {
        String initial = username == null || username.isBlank()
                ? "U"
                : username.substring(0, 1).toUpperCase();
        String safeInitial = initial
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 96 96">
                  <defs>
                    <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0" stop-color="#d8f3ef"/>
                      <stop offset="1" stop-color="#eef3f7"/>
                    </linearGradient>
                  </defs>
                  <rect width="96" height="96" rx="48" fill="url(#g)"/>
                  <text x="48" y="58" text-anchor="middle" font-family="Arial, sans-serif" font-size="38" font-weight="800" fill="#115e59">%s</text>
                </svg>
                """.formatted(safeInitial);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/svg+xml"))
                .cacheControl(CacheControl.noCache())
                .body(new ByteArrayResource(svg.getBytes()));
    }
}
