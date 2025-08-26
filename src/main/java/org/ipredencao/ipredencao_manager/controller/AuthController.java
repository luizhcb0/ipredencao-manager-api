package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.ErrorResponse;
import org.ipredencao.ipredencao_manager.model.auth.*;
import org.ipredencao.ipredencao_manager.service.AuthService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    @PostMapping("/login/google")
    public ResponseEntity<?> loginGoogle(@RequestBody LoginGoogleRequest request, HttpServletRequest httpRequest) {
        try {
            LoginResponse response = authService.loginComGoogle(request.getIdToken(), httpRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    @PostMapping("/login/facebook")
    public ResponseEntity<?> loginFacebook(@RequestBody LoginFacebookRequest request, HttpServletRequest httpRequest) {
        try {
            LoginResponse response = authService.loginComFacebook(request.getAccessToken(), httpRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (UnsupportedOperationException e) {
            return ResponseEntity.status(501).body(new ErrorResponse("Funcionalidade não implementada", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    @PostMapping("/login/apple")
    public ResponseEntity<?> loginApple(@RequestBody LoginAppleRequest request, HttpServletRequest httpRequest) {
        try {
            LoginResponse response = authService.loginComApple(
                request.getIdToken(), 
                request.getAuthorizationCode(),
                request.getUser(),
                httpRequest
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    @PostMapping("/login/email")
    public ResponseEntity<?> loginEmail(@RequestBody LoginEmailRequest request, HttpServletRequest httpRequest) {
        try {
            LoginResponse response = authService.loginComEmail(request.getIdToken(), httpRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        try {
            LoginResponse response = authService.register(request, httpRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            LoginResponse response = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        try {
            authService.logout(request.getRefreshToken());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body(new ErrorResponse("Auth service is running"));
    }
}
