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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    // ===== aula =====

    @Test
    void enrolledStudent_seesOnlyPublishedLessons() throws Exception {
        Long cycleId = createActiveCycleAsAdmin("Ciclo aulas 1");
        Long classId = createActiveNonFixedClassAsAdmin("Turma aulas 1", cycleId);
        createLessonAsAdmin(classId, "Aula rascunho");
        Long publishedLessonId = createLessonAsAdmin(classId, "Aula publicada");
        publishLessonAsAdmin(publishedLessonId, "Aula publicada");

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Aulas", Sexo.MASCULINO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/classes/" + classId + "/lessons").with(asUsuario(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Aula publicada"));
    }

    @Test
    void nonEnrolledCaller_cannotListLessons() throws Exception {
        Long cycleId = createActiveCycleAsAdmin("Ciclo aulas 2");
        Long classId = createActiveNonFixedClassAsAdmin("Turma aulas 2", cycleId);
        Usuario outsider = usuarioRepository.insert(UsuarioFixture.builder().build());

        mockMvc.perform(get(BASE + "/classes/" + classId + "/lessons").with(asUsuario(outsider)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void createLesson_isForbiddenForNonStaffNonTeacher() throws Exception {
        Long classId = createFixedClassAsAdmin("Turma aula forbidden");
        mockMvc.perform(post(BASE + "/classes/" + classId + "/lessons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Aula X\"}"))
                .andExpect(status().isForbidden());
    }

    // ===== material =====

    @Test
    void materialUpload_thenDownload_isAllowedForTeacher_andRejectedForOutsider() throws Exception {
        Long classId = createFixedClassAsAdmin("Turma materiais");
        Pessoa teacherPerson = PessoaFixture.membroComungante(pessoaService, "Professor Materiais", Sexo.FEMININO);
        Usuario teacherUser = usuarioRepository.insert(UsuarioFixture.builder().personId(teacherPerson.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + teacherPerson.getId() + ",\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk());

        MockMultipartFile file = new MockMultipartFile("file", "apostila.pdf", "application/pdf", "conteudo".getBytes());
        String body = mockMvc.perform(multipart(BASE + "/classes/" + classId + "/materials")
                        .file(file).with(asUsuario(teacherUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("apostila.pdf"))
                .andExpect(jsonPath("$.fileSizeBytes").value("conteudo".getBytes().length))
                .andReturn().getResponse().getContentAsString();
        Long materialId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get(BASE + "/materials/" + materialId + "/download").with(asUsuario(teacherUser)))
                .andExpect(status().isOk())
                .andExpect(content().bytes("conteudo".getBytes()));

        Pessoa outsiderPerson = PessoaFixture.membroComungante(pessoaService, "Estranho Materiais", Sexo.MASCULINO);
        Usuario outsiderUser = usuarioRepository.insert(UsuarioFixture.builder().personId(outsiderPerson.getId()).build());
        mockMvc.perform(get(BASE + "/materials/" + materialId + "/download").with(asUsuario(outsiderUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void lessonMaterial_isHiddenFromStudent_untilLessonIsPublished() throws Exception {
        Long cycleId = createActiveCycleAsAdmin("Ciclo material aula");
        Long classId = createActiveNonFixedClassAsAdmin("Turma material aula", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula com material");

        MockMultipartFile file = new MockMultipartFile("file", "slides.pdf", "application/pdf", "slides".getBytes());
        String body = mockMvc.perform(multipart(BASE + "/lessons/" + lessonId + "/materials").file(file).with(asAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long materialId = objectMapper.readTree(body).get("id").asLong();

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Material Aula", Sexo.FEMININO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        // aula ainda em rascunho: aluno matriculado não baixa o material dela
        mockMvc.perform(get(BASE + "/materials/" + materialId + "/download").with(asUsuario(studentUser)))
                .andExpect(status().isForbidden());

        publishLessonAsAdmin(lessonId, "Aula com material");

        mockMvc.perform(get(BASE + "/materials/" + materialId + "/download").with(asUsuario(studentUser)))
                .andExpect(status().isOk())
                .andExpect(content().bytes("slides".getBytes()));
    }

    // ===== presença =====

    @Test
    void selfReportAttendance_thenDuplicate_isRejected_andStaffSeesIt() throws Exception {
        Long cycleId = createActiveCycleAsAdmin("Ciclo presença 1");
        Long classId = createActiveNonFixedClassAsAdmin("Turma presença 1", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula com presença");
        publishLessonAsAdmin(lessonId, "Aula com presença");

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Presença", Sexo.FEMININO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance/me").with(asUsuario(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.present").value(true))
                .andExpect(jsonPath("$.selfReported").value(true))
                .andExpect(jsonPath("$.personId").value(student.getId()));

        mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance/me").with(asUsuario(studentUser)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(BASE + "/lessons/" + lessonId + "/attendance").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].present").value(true));
    }

    @Test
    void selfReportAttendance_onDraftLesson_isRejected() throws Exception {
        Long cycleId = createActiveCycleAsAdmin("Ciclo presença 2");
        Long classId = createActiveNonFixedClassAsAdmin("Turma presença 2", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula rascunho presença");

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Presença Rascunho", Sexo.MASCULINO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance/me").with(asUsuario(studentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void selfReportAttendance_withoutEnrollment_isRejected() throws Exception {
        Long cycleId = createActiveCycleAsAdmin("Ciclo presença 3");
        Long classId = createActiveNonFixedClassAsAdmin("Turma presença 3", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula sem matrícula");
        publishLessonAsAdmin(lessonId, "Aula sem matrícula");

        Pessoa outsider = PessoaFixture.membroComungante(pessoaService, "Não Matriculado", Sexo.FEMININO);
        Usuario outsiderUser = usuarioRepository.insert(UsuarioFixture.builder().personId(outsider.getId()).build());

        mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance/me").with(asUsuario(outsiderUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void rectifyAttendance_asStaff_flipsPresentAndClearsSelfReported() throws Exception {
        Long cycleId = createActiveCycleAsAdmin("Ciclo presença 4");
        Long classId = createActiveNonFixedClassAsAdmin("Turma presença 4", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula retificação");
        publishLessonAsAdmin(lessonId, "Aula retificação");

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Retificação", Sexo.MASCULINO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(studentUser)))
                .andExpect(status().isOk());
        String body = mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance/me").with(asUsuario(studentUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long attendanceId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(patch(BASE + "/attendance/" + attendanceId).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"present\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.present").value(false))
                .andExpect(jsonPath("$.selfReported").value(false));
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void listAttendance_isForbiddenForNonStaffNonTeacher() throws Exception {
        Long classId = createFixedClassAsAdmin("Turma presença forbidden");
        Long lessonId = createLessonAsAdmin(classId, "Aula forbidden");

        mockMvc.perform(get(BASE + "/lessons/" + lessonId + "/attendance"))
                .andExpect(status().isForbidden());
    }

    // ===== helpers =====

    private Long createLessonAsAdmin(Long classId, String title) throws Exception {
        String body = mockMvc.perform(post(BASE + "/classes/" + classId + "/lessons").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void publishLessonAsAdmin(Long lessonId, String title) throws Exception {
        mockMvc.perform(put(BASE + "/lessons/" + lessonId).with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk());
    }

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
