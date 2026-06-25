package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.user.dto.CreateUserRequest;
import org.ipredencao.ipredencao_manager.model.user.dto.UpdateUserRequest;
import org.ipredencao.ipredencao_manager.model.user.dto.UserSummaryResponse;
import org.ipredencao.ipredencao_manager.service.UserService;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<List<UserSummaryResponse>> listUsers(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) PerfilAcesso profile,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(userService.listUsers(active, profile, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserSummaryResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PostMapping
    public ResponseEntity<UserSummaryResponse> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserSummaryResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.updateUser(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        Long currentUserId = securityUtils.getCurrentUserId();
        userService.deleteUser(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
