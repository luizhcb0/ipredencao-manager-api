package org.ipredencao.ipredencao_manager.controller;

import com.google.firebase.auth.FirebaseAuthException;
import org.ipredencao.ipredencao_manager.model.ErrorResponse;
import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.model.user.UsuarioQuery;
import org.ipredencao.ipredencao_manager.model.user.dto.CreateUserRequest;
import org.ipredencao.ipredencao_manager.model.user.dto.UpdateUserRequest;
import org.ipredencao.ipredencao_manager.model.user.dto.UserSummaryResponse;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.ipredencao.ipredencao_manager.jooq.tables.Pessoa.PESSOA;
import static org.ipredencao.ipredencao_manager.jooq.tables.PessoaHistory.PESSOA_HISTORY;
import static org.ipredencao.ipredencao_manager.service.UserService.CANNOT_DELETE_USER_WITH_REFERENCES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @BeforeEach
    void setUp() throws FirebaseAuthException {
        usuarioRepository.insert(UsuarioFixture.builder()
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

        UserSummaryResponse[] users = read(mockMvc.perform(get(BASE)).andExpect(status().isOk()), UserSummaryResponse[].class);
        assertTrue(users.length >= 2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsCreated() throws Exception {
        UserSummaryResponse created = read(postUser(new CreateUserRequest(
            "Novo Usuario", "novo@test.local", PerfilAcesso.BOLETIM, null
        )).andExpect(status().isCreated()), UserSummaryResponse.class);

        assertEquals("novo@test.local", created.email());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsConflictForDuplicateEmail() throws Exception {
        usuarioRepository.insert(UsuarioFixture.builder()
            .email("duplicado@test.local")
            .build());

        postUser(new CreateUserRequest("Outro", "duplicado@test.local", PerfilAcesso.BOLETIM, null))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_linksExistingFirebaseAccountWhenNotInDb() throws Exception {
        when(firebaseAuthService.createUserWithoutPassword(eq("mateus@test.local"), any()))
            .thenThrow(new RuntimeException(
                "Firebase Lambda error: The user with the provided email already exists (EMAIL_EXISTS)."));
        when(firebaseAuthService.getUserByEmail("mateus@test.local"))
            .thenReturn(new FirebaseUser("existing-firebase-uid", "mateus@test.local", "Mateus", true, null, false));

        UserSummaryResponse created = read(postUser(new CreateUserRequest(
            "Mateus Souza", "mateus@test.local", PerfilAcesso.PRESBITERO, null
        )).andExpect(status().isCreated()), UserSummaryResponse.class);

        assertEquals("mateus@test.local", created.email());
        assertEquals("Mateus Souza", created.name());
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

        UpdateUserRequest body = new UpdateUserRequest();
        body.setAccessProfile(PerfilAcesso.DIACONO);

        UserSummaryResponse updated = read(patchUser(user.getId(), body).andExpect(status().isOk()), UserSummaryResponse.class);
        assertEquals(PerfilAcesso.DIACONO, updated.accessProfile());
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

        assertNull(findUser(user.getId()));
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

        ErrorResponse error = read(mockMvc.perform(delete(BASE + "/" + user.getId()))
            .andExpect(status().isConflict()), ErrorResponse.class);
        assertEquals(CANNOT_DELETE_USER_WITH_REFERENCES, error.getMessage());

        verify(firebaseAuthService, never()).deleteUser(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_linksPersonWhenPersonIdProvided() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Pessoa Convite", Sexo.FEMININO);

        UserSummaryResponse created = read(postUser(new CreateUserRequest(
            "Novo Usuario", "novo@test.local", PerfilAcesso.BOLETIM, pessoa.getId()
        )).andExpect(status().isCreated()), UserSummaryResponse.class);

        assertEquals(pessoa.getId(), created.personId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsBadRequestWhenPersonMissing() throws Exception {
        ErrorResponse error = read(postUser(new CreateUserRequest(
            "Novo Usuario", "novo@test.local", PerfilAcesso.BOLETIM, 999_999L
        )).andExpect(status().isBadRequest()), ErrorResponse.class);

        assertEquals(UserService.PERSON_NOT_FOUND, error.getMessage());
        verify(firebaseAuthService, never()).createUserWithoutPassword(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returnsConflictWhenPersonAlreadyLinked() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Pessoa Duplicada", Sexo.MASCULINO);
        usuarioRepository.insert(UsuarioFixture.builder()
            .email("ja-vinculado@test.local")
            .personId(pessoa.getId())
            .build());

        ErrorResponse error = read(postUser(new CreateUserRequest(
            "Outro", "outro@test.local", PerfilAcesso.BOLETIM, pessoa.getId()
        )).andExpect(status().isConflict()), ErrorResponse.class);

        assertEquals(UserService.PERSON_ALREADY_LINKED, error.getMessage());
        verify(firebaseAuthService, never()).createUserWithoutPassword(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patch_linksAndClearsPerson() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Pessoa Patch", Sexo.FEMININO);
        Usuario user = usuarioRepository.insert(UsuarioFixture.builder()
            .email("patch-person@test.local")
            .firebaseUid("patch-person-uid")
            .build());

        UpdateUserRequest link = new UpdateUserRequest();
        link.setPersonId(pessoa.getId());
        UserSummaryResponse linked = read(patchUser(user.getId(), link).andExpect(status().isOk()), UserSummaryResponse.class);
        assertEquals(pessoa.getId(), linked.personId());

        UpdateUserRequest rename = new UpdateUserRequest();
        rename.setName("Ainda vinculado");
        UserSummaryResponse kept = read(patchUser(user.getId(), rename).andExpect(status().isOk()), UserSummaryResponse.class);
        assertEquals(pessoa.getId(), kept.personId());
        assertEquals("Ainda vinculado", kept.name());

        UpdateUserRequest unlink = new UpdateUserRequest();
        unlink.setPersonId(null);
        UserSummaryResponse cleared = read(patchUser(user.getId(), unlink).andExpect(status().isOk()), UserSummaryResponse.class);
        assertNull(cleared.personId());
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

        Usuario reloaded = findUser(user.getId());
        assertNull(reloaded.getPersonId());
    }

    private ResultActions postUser(CreateUserRequest request) throws Exception {
        return mockMvc.perform(post(BASE)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions patchUser(Long id, UpdateUserRequest request) throws Exception {
        return mockMvc.perform(patch(BASE + "/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
    }

    private <T> T read(ResultActions actions, Class<T> type) throws Exception {
        return objectMapper.readValue(actions.andReturn().getResponse().getContentAsString(), type);
    }

    private Usuario findUser(Long id) {
        return usuarioRepository.find(UsuarioQuery.builder().id(id).build())
            .stream().findFirst().orElse(null);
    }
}
