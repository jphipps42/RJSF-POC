package com.egs.rjsf.controller;

import com.egs.rjsf.entity.AppUser;
import com.egs.rjsf.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        AppUser user = userRepository.findByUsernameAndIsActiveTrue(request.username())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "display_name", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                "role", user.getRole(),
                "organization", user.getOrganization()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
        try {
            var uuid = java.util.UUID.fromString(userId);
            return userRepository.findById(uuid)
                    .filter(AppUser::getIsActive)
                    .map(user -> ResponseEntity.ok(Map.of(
                            "id", (Object) user.getId(),
                            "username", user.getUsername(),
                            "display_name", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                            "role", user.getRole(),
                            "organization", user.getOrganization()
                    )))
                    .orElse(ResponseEntity.status(401).body(Map.of("error", "User not found")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid user ID"));
        }
    }
}
