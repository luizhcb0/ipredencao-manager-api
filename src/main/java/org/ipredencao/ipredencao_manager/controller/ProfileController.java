package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.auth.UserProfile;
import org.ipredencao.ipredencao_manager.model.user.dto.UpdateProfileRequest;
import org.ipredencao.ipredencao_manager.service.UserService;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<UserProfile> getProfile() {
        Long userId = requireCurrentUserId();
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PatchMapping
    public ResponseEntity<UserProfile> updateProfile(@RequestBody UpdateProfileRequest request) {
        Long userId = requireCurrentUserId();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    private Long requireCurrentUserId() {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw new IllegalArgumentException("Usuário não autenticado");
        }
        return userId;
    }
}
