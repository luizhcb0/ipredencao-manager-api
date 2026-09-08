package org.ipredencao.ipredencao_manager.controller;

import com.google.firebase.auth.FirebaseAuthException;
import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.repository.UsuarioRepository;
import org.ipredencao.ipredencao_manager.service.FirebaseAuthService;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.ipredencao.ipredencao_manager.service.UserService;
import org.ipredencao.ipredencao_manager.service.firebase.FirebaseUser;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.ipredencao.ipredencao_manager.support.UsuarioFixture;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.ipredencao.ipredencao_manager.jooq.tables.Pessoa.PESSOA;
import static org.ipredencao.ipredencao_manager.jooq.tables.PessoaHistory.PESSOA_HISTORY;
import static org.ipredencao.ipredencao_manager.service.UserService.CANNOT_DELETE_USER_WITH_REFERENCES;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class UserControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/users";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PessoaService pessoaService;

    @Autowired
    private DSLContext dsl;

    @MockitoBean
    private FirebaseAuthService firebaseAuthService;

    private Usuario adminUser;

    @BeforeEach
    void setUp() throws FirebaseAuthException {
        adminUser = usuarioRepository.insert(UsuarioFixture.builder()
            .email("admin@test.local")
            .name("Admin Teste")
            .accessProfile(PerfilAcesso.ADMIN)
            .firebaseUid("admin-firebase-uid")
            .build());

        when(firebaseAuthService.createUserWithoutPassword(any(), any()))
            .thenReturn(new FirebaseUser("new-firebase-uid", "novo@test.local", "Novo Usuario", false, null, false));
        doNothing().when(firebaseAuthService).updateUser(any(), any());
        doNothing().when(firebaseAuthService).setUserDisabled(any(), eq(true));
        doNothing().when(firebaseAuthService).deleteUser(any());
    }

    @Test
    void list_returnsUnauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(get(BASE))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void list_returnsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(get(BASE))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_returnsUsersForAdmin() throws Exception {
        usuarioRepository.insert(UsuarioFixture.builder()
            .email("boletim@test.local")
            .accessProfile(PerfilAcesso.BOLETIM)
            .build());

        mockMvc.perform(get(BASE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsCreated() throws Exception {
        mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Novo Usuario",
                    "email", "novo@test.local",
                    "profile", "BOLETIM"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("novo@test.local"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsConflictForDuplicateEmail() throws Exception {
        usuarioRepository.insert(UsuarioFixture.builder()
            .email("duplicado@test.local")
            .build());

        mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Outro",
                    "email", "duplicado@test.local",
                    "profile", "BOLETIM"
                ))))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_linksExistingFirebaseAccountWhenNotInDb() throws Exception {
        // Caminho Lambda: o erro do Firebase chega embrulhado em RuntimeException
        when(firebaseAuthService.createUserWithoutPassword(eq("mateus@test.local"), any()))
            .thenThrow(new RuntimeException(
                "Firebase Lambda error: The user with the provided email already exists (EMAIL_EXISTS)."));
        when(firebaseAuthService.getUserByEmail("mateus@test.local"))
            .thenReturn(new FirebaseUser("existing-firebase-uid", "mateus@test.local", "Mateus", true, null, false));

        mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Mateus Souza",
                    "email", "mateus@test.local",
                    "profile", "PRESBITERO"
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("mateus@test.local"))
            .andExpect(jsonPath("$.name").value("Mateus Souza"));

        verify(firebaseAuthService, never()).deleteUser(any());
        verify(firebaseAuthService).updateUser("existing-firebase-uid", "Mateus Souza");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patch_updatesUserProfile() throws Exception {
        Usuario user = usuarioRepository.insert(UsuarioFixture.builder()
            .email("patch@test.local")
            .accessProfile(PerfilAcesso.BOLETIM)
            .firebaseUid("patch-firebase-uid")
            .build());

        mockMvc.perform(patch(BASE + "/" + user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "accessProfile", "DIACONO"
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessProfile").value("DIACONO"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_removesUserWithoutReferences() throws Exception {
        Usuario user = usuarioRepository.insert(UsuarioFixture.builder()
            .email("delete@test.local")
            .firebaseUid("delete-firebase-uid")
            .build());

        mockMvc.perform(delete(BASE + "/" + user.getId()))
            .andExpect(status().isNoContent());

        assertNull(usuarioRepository.findById(user.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returnsConflictWhenUserHasReferences() throws Exception {
        Usuario user = usuarioRepository.insert(UsuarioFixture.builder()
            .email("referenced@test.local")
            .firebaseUid("referenced-firebase-uid")
            .build());

        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Pessoa Referencia", Sexo.MASCULINO);
        dsl.update(PESSOA)
            .set(PESSOA.UPDATED_BY, user.getId())
            .where(PESSOA.PESSOA_ID.eq(pessoa.getId()))
            .execute();

        mockMvc.perform(delete(BASE + "/" + user.getId()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(CANNOT_DELETE_USER_WITH_REFERENCES));

        verify(firebaseAuthService, never()).deleteUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_linksPersonWhenPersonIdProvided() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Pessoa Convite", Sexo.FEMININO);

        mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Novo Usuario",
                    "email", "novo@test.local",
                    "profile", "BOLETIM",
                    "personId", pessoa.getId()
                ))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.personId").value(pessoa.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsBadRequestWhenPersonMissing() throws Exception {
        mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Novo Usuario",
                    "email", "novo@test.local",
                    "profile", "BOLETIM",
                    "personId", 999_999L
                ))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(UserService.PERSON_NOT_FOUND));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsConflictWhenPersonAlreadyLinked() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Pessoa Duplicada", Sexo.MASCULINO);
        usuarioRepository.insert(UsuarioFixture.builder()
            .email("ja-vinculado@test.local")
            .personId(pessoa.getId())
            .build());

        mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "name", "Outro",
                    "email", "outro@test.local",
                    "profile", "BOLETIM",
                    "personId", pessoa.getId()
                ))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(UserService.PERSON_ALREADY_LINKED));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patch_linksAndClearsPerson() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Pessoa Patch", Sexo.FEMININO);
        Usuario user = usuarioRepository.insert(UsuarioFixture.builder()
            .email("patch-person@test.local")
            .firebaseUid("patch-person-uid")
            .build());

        mockMvc.perform(patch(BASE + "/" + user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("personId", pessoa.getId()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.personId").value(pessoa.getId()));

        mockMvc.perform(patch(BASE + "/" + user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ainda vinculado\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.personId").value(pessoa.getId()))
            .andExpect(jsonPath("$.name").value("Ainda vinculado"));

        mockMvc.perform(patch(BASE + "/" + user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"personId\":null}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.personId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletingPerson_nullsUserPersonId() {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Pessoa Apagar", Sexo.MASCULINO);
        Usuario user = usuarioRepository.insert(UsuarioFixture.builder()
            .email("set-null@test.local")
            .personId(pessoa.getId())
            .build());

        dsl.deleteFrom(PESSOA_HISTORY).where(PESSOA_HISTORY.PESSOA_ID.eq(pessoa.getId())).execute();
        dsl.deleteFrom(PESSOA).where(PESSOA.PESSOA_ID.eq(pessoa.getId())).execute();

        Usuario reloaded = usuarioRepository.findById(user.getId());
        assertNull(reloaded.getPersonId());
    }
}
