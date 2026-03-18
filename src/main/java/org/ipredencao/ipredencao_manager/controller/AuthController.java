package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.auth.dto.LoginAppleRequest;
import org.ipredencao.ipredencao_manager.model.auth.dto.LoginEmailRequest;
import org.ipredencao.ipredencao_manager.model.auth.dto.LoginFacebookRequest;
import org.ipredencao.ipredencao_manager.model.auth.dto.LoginGoogleRequest;
import org.ipredencao.ipredencao_manager.model.auth.dto.LoginResponse;
import org.ipredencao.ipredencao_manager.model.auth.dto.LogoutRequest;
import org.ipredencao.ipredencao_manager.model.auth.dto.RefreshTokenRequest;
import org.ipredencao.ipredencao_manager.model.auth.dto.RegisterRequest;
import org.ipredencao.ipredencao_manager.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login/google")
    public ResponseEntity<LoginResponse> loginWithGoogle(@RequestBody LoginGoogleRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.loginWithGoogle(request.getIdToken(), httpRequest));
    }

    @PostMapping("/login/facebook")
    public ResponseEntity<LoginResponse> loginWithFacebook(@RequestBody LoginFacebookRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.loginWithFacebook(request.getAccessToken(), httpRequest));
    }

    @PostMapping("/login/apple")
    public ResponseEntity<LoginResponse> loginWithApple(@RequestBody LoginAppleRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.loginWithApple(
                request.getIdToken(),
                request.getAuthorizationCode(),
                request.getUser(),
                httpRequest));
    }

    @PostMapping("/login/email")
    public ResponseEntity<LoginResponse> loginWithEmail(@RequestBody LoginEmailRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.loginWithEmail(request.getIdToken(), httpRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(authService.register(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody LogoutRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "up"));
    }
}
