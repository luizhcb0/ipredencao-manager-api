package org.ipredencao.ipredencao_manager.controller;

import com.google.firebase.auth.FirebaseAuthException;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.repository.UsuarioRepository;
import org.ipredencao.ipredencao_manager.service.FirebaseAuthService;
import org.ipredencao.ipredencao_manager.service.JwtService;
import org.ipredencao.ipredencao_manager.service.firebase.FirebaseUser;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.UsuarioFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ProfileControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/me";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private FirebaseAuthService firebaseAuthService;

    @BeforeEach
    void setUp() throws FirebaseAuthException {
        when(firebaseAuthService.getUserByUid(any()))
            .thenReturn(new FirebaseUser("uid", "user@test.local", "User", true, "https://photo.test/a.jpg", false));
        doNothing().when(firebaseAuthService).updateUser(any(), any());
    }

    @Test
    void getProfile_returnsUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get(BASE))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_returnsProfileWhenAuthenticated() throws Exception {
        Usuario user = usuarioRepository.insert(UsuarioFixture.builder()
            .email("profile@test.local")
            .firebaseUid("profile-firebase-uid")
            .build());

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + jwtService.generateToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("profile@test.local"))
            .andExpect(jsonPath("$.fotoUrl").value("https://photo.test/a.jpg"));
    }

    @Test
    void patchProfile_updatesName() throws Exception {
        Usuario user = usuarioRepository.insert(UsuarioFixture.builder()
            .email("rename@test.local")
            .firebaseUid("rename-firebase-uid")
            .build());

        mockMvc.perform(patch(BASE)
                .header("Authorization", "Bearer " + jwtService.generateToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name", "Nome Atualizado"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Nome Atualizado"));
    }
}
