package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.auth.AuthUser;
import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.repository.UsuarioRepository;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.ipredencao.ipredencao_manager.support.UsuarioFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 1 da EBD (docs/EBD_ANALISE_E_PLANO.md): ciclo, turma e matrícula.
 * Cobre a matriz de permissões (STAFF / professor-da-turma via {@code EbdAccess}
 * / automatrícula) e as regras confirmadas com o usuário (1 ciclo ativo por
 * vez; turma fixa só ativa com professor; STUDENT só por matrícula
 * administrativa em turma fixa ou por automatrícula em turma não-fixa).
 */
@Transactional
class EbdControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/ebd";

    @Autowired private PessoaService pessoaService;
    @Autowired private UsuarioRepository usuarioRepository;

    // ===== security matrix =====

    @Test
    void listCycles_returnsUnauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(get(BASE + "/cycles")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void listCycles_isAllowedForMember() throws Exception {
        // MEMBER fica de fora de Roles.anyNames() (V014) — a leitura de /api/ebd/**
        // precisa continuar acessível a ele mesmo assim (ver comentário em EbdController).
        mockMvc.perform(get(BASE + "/cycles")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void createCycle_isForbiddenForNonStaff() throws Exception {
        mockMvc.perform(post(BASE + "/cycles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ciclo X\"}"))
                .andExpect(status().isForbidden());
    }

    // ===== ciclo =====

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCycle_active_thenSecondActive_isRejected() throws Exception {
        mockMvc.perform(post(BASE + "/cycles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"2026 - 1º semestre\",\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post(BASE + "/cycles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"2026 - 2º semestre\",\"active\":true}"))
                .andExpect(status().isBadRequest());
    }

    // ===== turma =====

    @Test
    @WithMockUser(roles = "ADMIN")
    void createFixedClass_withoutCycle_isAllowed() throws Exception {
        mockMvc.perform(post(BASE + "/classes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Turma dos Adultos\",\"fixed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.fixed").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createNonFixedClass_withoutCycle_isRejected() throws Exception {
        mockMvc.perform(post(BASE + "/classes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Turma do ciclo\",\"fixed\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activateFixedClass_withoutTeacher_isRejected() throws Exception {
        Long classId = createFixedClass("Sem professor");

        mockMvc.perform(put(BASE + "/classes/" + classId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sem professor\",\"fixed\":true,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void activateFixedClass_withTeacher_isAllowed() throws Exception {
        Long classId = createFixedClass("Com professor");
        Pessoa teacher = PessoaFixture.membroComungante(pessoaService, "Professor API", Sexo.MASCULINO);

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + teacher.getId() + ",\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put(BASE + "/classes/" + classId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Com professor\",\"fixed\":true,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ===== matrícula administrativa =====

    @Test
    @WithMockUser(roles = "BOLETIM")
    void addTeacher_isForbiddenForNonStaff() throws Exception {
        Long classId = createFixedClassAsAdmin("Turma professor forbidden");
        Pessoa person = PessoaFixture.membroComungante(pessoaService, "Alguem", Sexo.FEMININO);

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + person.getId() + ",\"role\":\"TEACHER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addStudent_toNonFixedClass_viaAdminEndpoint_isRejected() throws Exception {
        Long cycleId = createActiveCycleAsAdmin("Ciclo matrícula");
        Long classId = createNonFixedClassAsAdmin("Turma não-fixa", cycleId);
        Pessoa person = PessoaFixture.membroComungante(pessoaService, "Aluno via admin", Sexo.MASCULINO);

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + person.getId() + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void teacherOfClass_canAddStudent_evenWithoutStaffRole() throws Exception {
        Long classId = createFixedClassAsAdmin("Turma com professor logado");
        Pessoa teacherPerson = PessoaFixture.membroComungante(pessoaService, "Professor Logado", Sexo.MASCULINO);
        Usuario teacherUser = usuarioRepository.insert(UsuarioFixture.builder()
                .accessProfile(PerfilAcesso.BOLETIM) // não-STAFF: prova que o acesso vem do vínculo, não do perfil
                .personId(teacherPerson.getId())
                .build());

        // ADMIN associa a pessoa como professora da turma.
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + teacherPerson.getId() + ",\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk());

        // O professor (BOLETIM, não-STAFF) inclui um aluno na própria turma fixa.
        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno via professor", Sexo.FEMININO);
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(teacherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + student.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    // ===== automatrícula (turma não-fixa) =====

    @Test
    void selfEnroll_withoutPersonLinked_isRejected() throws Exception {
        Long cycleId = createActiveCycleAsAdmin("Ciclo automatrícula 1");
        Long classId = createActiveNonFixedClassAsAdmin("Turma automatrícula 1", cycleId);
        Usuario noPersonUser = usuarioRepository.insert(UsuarioFixture.builder().personId(null).build());

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(noPersonUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void selfEnroll_thenDuplicate_isRejected_andSelfUnenroll_removesIt() throws Exception {
        Long cycleId = createActiveCycleAsAdmin("Ciclo automatrícula 2");
        Long classId = createActiveNonFixedClassAsAdmin("Turma automatrícula 2", cycleId);
        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Automatrícula", Sexo.FEMININO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder()
                .accessProfile(PerfilAcesso.MEMBER)
                .personId(student.getId())
                .build());

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(studentUser)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(studentUser)))
                .andExpect(status().isNoContent());

        // Depois de cancelar, matricula de novo com sucesso (prova que o delete funcionou).
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(studentUser)))
                .andExpect(status().isOk());
    }

    @Test
    void selfEnroll_intoFixedClass_isRejected() throws Exception {
        Long classId = createFixedClassAsAdmin("Turma fixa automatrícula");
        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Turma Fixa", Sexo.MASCULINO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(studentUser)))
                .andExpect(status().isBadRequest());
    }

    // ===== visibilidade =====

    @Test
    @WithMockUser(roles = "BOLETIM")
    void getDraftClass_isNotFoundForNonPrivilegedCaller() throws Exception {
        Long classId = createFixedClassAsAdmin("Turma rascunho oculta");
        mockMvc.perform(get(BASE + "/classes/" + classId)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void getActiveClass_isVisibleToAnyAuthenticated() throws Exception {
        Long cycleId = createActiveCycleAsAdmin("Ciclo visibilidade");
        Long classId = createActiveNonFixedClassAsAdmin("Turma visível", cycleId);
        mockMvc.perform(get(BASE + "/classes/" + classId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Turma visível"));
    }

    // ===== helpers =====

    private Long createFixedClass(String name) throws Exception {
        String body = mockMvc.perform(post(BASE + "/classes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"fixed\":true}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private RequestPostProcessor asAdmin() {
        Usuario admin = usuarioRepository.insert(UsuarioFixture.builder().accessProfile(PerfilAcesso.ADMIN).build());
        return asUsuario(admin);
    }

    private Long createFixedClassAsAdmin(String name) throws Exception {
        String body = mockMvc.perform(post(BASE + "/classes").with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"fixed\":true}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createActiveCycleAsAdmin(String name) throws Exception {
        String body = mockMvc.perform(post(BASE + "/cycles").with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createNonFixedClassAsAdmin(String name, Long cycleId) throws Exception {
        String body = mockMvc.perform(post(BASE + "/classes").with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"fixed\":false,\"cycleId\":" + cycleId + "}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createActiveNonFixedClassAsAdmin(String name, Long cycleId) throws Exception {
        Long classId = createNonFixedClassAsAdmin(name, cycleId);
        mockMvc.perform(put(BASE + "/classes/" + classId).with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"fixed\":false,\"cycleId\":" + cycleId
                                + ",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        return classId;
    }

    private RequestPostProcessor asUsuario(Usuario usuario) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AuthUser(usuario.getId(), usuario.getEmail(), usuario.getAccessProfile()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getAccessProfile().name())));
        return authentication(authentication);
    }
}
