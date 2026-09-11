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
 * EBD (docs/EBD_ANALISE_E_PLANO.md): ciclo, turma, matrícula, aula, material e
 * presença. Toda turma pertence a um ciclo (cycleId obrigatório) — não existe
 * mais distinção "turma fixa" (removida a pedido do time em revisão do PR).
 * Matrícula e presença usam endpoints únicos que servem STAFF e o próprio
 * interessado: personId no body presente = ação administrativa (STAFF);
 * ausente = self-service (ver EbdService.addEnrollment/markAttendance).
 */
@Transactional
class EbdControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/ebd";
    private static final String LESSON_DATE = "\"lessonDate\":\"2026-01-01\"";

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
    void createClass_withoutCycle_isRejected() throws Exception {
        mockMvc.perform(post(BASE + "/classes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Turma sem ciclo\"}"))
                .andExpect(status().isBadRequest());
    }

    // Sem exigência de professor vinculado pra ativar — essa regra existia só
    // pra turma fixa (conceito removido); toda turma agora ativa direto, como
    // as não-fixas de antes.
    @Test
    void activateClass_withoutTeacher_isAllowed() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo ativação");
        Long classId = createClassAsAdmin("Turma sem professor", cycleId);

        mockMvc.perform(put(BASE + "/classes/" + classId).with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Turma sem professor\",\"cycleId\":" + cycleId + ",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ===== matrícula administrativa =====

    @Test
    @WithMockUser(roles = "BOLETIM")
    void addTeacher_isForbiddenForNonStaff() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo professor forbidden");
        Long classId = createClassAsAdmin("Turma professor forbidden", cycleId);
        Pessoa person = PessoaFixture.membroComungante(pessoaService, "Alguem", Sexo.FEMININO);

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + person.getId() + ",\"role\":\"TEACHER\"}"))
                .andExpect(status().isForbidden());
    }

    // Prova a correção do gap relatado em revisão: STAFF/professor consegue
    // incluir um aluno manualmente em qualquer turma (antes disso era rejeitado
    // fora da "turma fixa", conceito removido).
    @Test
    void addStudent_viaAdminEndpoint_isAllowed() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo matrícula admin");
        Long classId = createClassAsAdmin("Turma matrícula admin", cycleId);
        Pessoa person = PessoaFixture.membroComungante(pessoaService, "Aluno via admin", Sexo.MASCULINO);

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + person.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void teacherOfClass_withoutStaffRole_cannotAddStudent() throws Exception {
        // Simplificação pedida em revisão do PR: o vínculo TEACHER, sozinho, não
        // concede mais permissão de gerenciar a turma — só STAFF (diácono/
        // presbítero/admin) gerencia, mesmo sendo o professor dela.
        Long cycleId = createCycleAsAdmin("Ciclo professor não-staff");
        Long classId = createClassAsAdmin("Turma com professor logado", cycleId);
        Pessoa teacherPerson = PessoaFixture.membroComungante(pessoaService, "Professor Logado", Sexo.MASCULINO);
        Usuario teacherUser = usuarioRepository.insert(UsuarioFixture.builder()
                .accessProfile(PerfilAcesso.BOLETIM) // não-STAFF
                .personId(teacherPerson.getId())
                .build());

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + teacherPerson.getId() + ",\"role\":\"TEACHER\"}"))
                .andExpect(status().isOk());

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno via professor", Sexo.FEMININO);
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(teacherUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + student.getId() + "}"))
                .andExpect(status().isForbidden());
    }

    // ===== automatrícula =====

    @Test
    void selfEnroll_withoutPersonLinked_isRejected() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo automatrícula 1");
        Long classId = createActiveClassAsAdmin("Turma automatrícula 1", cycleId);
        Usuario noPersonUser = usuarioRepository.insert(UsuarioFixture.builder().personId(null).build());

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(noPersonUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void selfEnroll_thenDuplicate_isRejected_andSelfDelete_removesIt() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo automatrícula 2");
        Long classId = createActiveClassAsAdmin("Turma automatrícula 2", cycleId);
        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Automatrícula", Sexo.FEMININO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder()
                .accessProfile(PerfilAcesso.MEMBER)
                .personId(student.getId())
                .build());

        String body = mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andReturn().getResponse().getContentAsString();
        Long enrollmentId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isBadRequest());

        // Remove pelo endpoint colapsado (id direto), não só pelo atalho /me —
        // prova que o aluno também pode usar a mesma rota administrativa na
        // própria matrícula.
        mockMvc.perform(delete(BASE + "/classes/" + classId + "/enrollments/" + enrollmentId).with(asUsuario(studentUser)))
                .andExpect(status().isNoContent());

        // Depois de cancelar, matricula de novo com sucesso (prova que o delete funcionou).
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isOk());
    }

    @Test
    void selfEnroll_intoInactiveClass_isRejected() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo automatrícula inativa");
        Long classId = createClassAsAdmin("Turma rascunho automatrícula", cycleId);
        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Turma Rascunho", Sexo.MASCULINO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void selfUnenroll_removesOwnEnrollment() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo automatrícula /me");
        Long classId = createActiveClassAsAdmin("Turma automatrícula /me", cycleId);
        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno /me", Sexo.FEMININO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());

        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        mockMvc.perform(delete(BASE + "/classes/" + classId + "/enrollments/me").with(asUsuario(studentUser)))
                .andExpect(status().isNoContent());
    }

    // ===== visibilidade =====

    @Test
    @WithMockUser(roles = "BOLETIM")
    void getDraftClass_isNotFoundForNonPrivilegedCaller() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo turma rascunho");
        Long classId = createClassAsAdmin("Turma rascunho oculta", cycleId);
        mockMvc.perform(get(BASE + "/classes/" + classId)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void getActiveClass_isVisibleToAnyAuthenticated() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo turma visível");
        Long classId = createActiveClassAsAdmin("Turma visível", cycleId);
        mockMvc.perform(get(BASE + "/classes/" + classId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Turma visível"));
    }

    // ===== aula =====

    @Test
    void enrolledStudent_seesOnlyPublishedLessons() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo aulas 1");
        Long classId = createActiveClassAsAdmin("Turma aulas 1", cycleId);
        createLessonAsAdmin(classId, "Aula rascunho");
        Long publishedLessonId = createLessonAsAdmin(classId, "Aula publicada");
        publishLessonAsAdmin(publishedLessonId, "Aula publicada");

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Aulas", Sexo.MASCULINO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/classes/" + classId + "/lessons").with(asUsuario(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Aula publicada"));
    }

    @Test
    void nonEnrolledCaller_cannotListLessons() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo aulas 2");
        Long classId = createActiveClassAsAdmin("Turma aulas 2", cycleId);
        Usuario outsider = usuarioRepository.insert(UsuarioFixture.builder().build());

        mockMvc.perform(get(BASE + "/classes/" + classId + "/lessons").with(asUsuario(outsider)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void createLesson_isForbiddenForNonStaff() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo aula forbidden");
        Long classId = createClassAsAdmin("Turma aula forbidden", cycleId);
        mockMvc.perform(post(BASE + "/classes/" + classId + "/lessons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Aula X\"," + LESSON_DATE + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createLesson_withoutLessonDate_isRejected() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo aula sem data");
        Long classId = createClassAsAdmin("Turma aula sem data", cycleId);
        mockMvc.perform(post(BASE + "/classes/" + classId + "/lessons").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Aula sem data\"}"))
                .andExpect(status().isBadRequest());
    }

    // ===== material =====

    @Test
    void materialUpload_thenDownload_isAllowedForStaff_andRejectedForOutsider() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo materiais");
        Long classId = createClassAsAdmin("Turma materiais", cycleId);
        Pessoa staffPerson = PessoaFixture.membroComungante(pessoaService, "Diácono Materiais", Sexo.FEMININO);
        Usuario staffUser = usuarioRepository.insert(UsuarioFixture.builder()
                .accessProfile(PerfilAcesso.DIACONO)
                .personId(staffPerson.getId())
                .build());

        MockMultipartFile file = new MockMultipartFile("file", "apostila.pdf", "application/pdf", "conteudo".getBytes());
        String body = mockMvc.perform(multipart(BASE + "/classes/" + classId + "/materials")
                        .file(file).with(asUsuario(staffUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("apostila.pdf"))
                .andExpect(jsonPath("$.fileSizeBytes").value("conteudo".getBytes().length))
                .andReturn().getResponse().getContentAsString();
        Long materialId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get(BASE + "/materials/" + materialId + "/download").with(asUsuario(staffUser)))
                .andExpect(status().isOk())
                .andExpect(content().bytes("conteudo".getBytes()));

        Pessoa outsiderPerson = PessoaFixture.membroComungante(pessoaService, "Estranho Materiais", Sexo.MASCULINO);
        Usuario outsiderUser = usuarioRepository.insert(UsuarioFixture.builder().personId(outsiderPerson.getId()).build());
        mockMvc.perform(get(BASE + "/materials/" + materialId + "/download").with(asUsuario(outsiderUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void lessonMaterial_isHiddenFromStudent_untilLessonIsPublished() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo material aula");
        Long classId = createActiveClassAsAdmin("Turma material aula", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula com material");

        MockMultipartFile file = new MockMultipartFile("file", "slides.pdf", "application/pdf", "slides".getBytes());
        String body = mockMvc.perform(multipart(BASE + "/lessons/" + lessonId + "/materials").file(file).with(asAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long materialId = objectMapper.readTree(body).get("id").asLong();

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Material Aula", Sexo.FEMININO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
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
    void markAttendance_thenDuplicate_isRejected_andStaffSeesIt() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo presença 1");
        Long classId = createActiveClassAsAdmin("Turma presença 1", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula com presença");
        publishLessonAsAdmin(lessonId, "Aula com presença");

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Presença", Sexo.FEMININO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance").with(asUsuario(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.present").value(true))
                .andExpect(jsonPath("$.personId").value(student.getId()));

        mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance").with(asUsuario(studentUser)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(BASE + "/lessons/" + lessonId + "/attendance").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].present").value(true));
    }

    // Ponto levantado em revisão do PR: o professor/STAFF também pode marcar
    // presença em nome do aluno (personId no body), não só o próprio aluno.
    @Test
    void markAttendance_byStaffForStudent_isAllowed() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo presença staff");
        Long classId = createActiveClassAsAdmin("Turma presença staff", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula presença staff");
        publishLessonAsAdmin(lessonId, "Aula presença staff");

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Marcado Por Staff", Sexo.MASCULINO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + student.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personId").value(student.getId()));
    }

    // Ponto levantado em revisão do PR: o aluno também pode remover a própria
    // presença a qualquer momento (antes só existia marcar, nunca desmarcar).
    @Test
    void markAttendance_thenSelfDelete_isAllowed() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo presença remoção");
        Long classId = createActiveClassAsAdmin("Turma presença remoção", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula presença remoção");
        publishLessonAsAdmin(lessonId, "Aula presença remoção");

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Remove Presença", Sexo.FEMININO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        String body = mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance").with(asUsuario(studentUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long attendanceId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(delete(BASE + "/attendance/" + attendanceId).with(asUsuario(studentUser)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/lessons/" + lessonId + "/attendance/me").with(asUsuario(studentUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSelfAttendance_beforeAndAfterMarking() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo presença própria");
        Long classId = createActiveClassAsAdmin("Turma presença própria", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula consulta presença");
        publishLessonAsAdmin(lessonId, "Aula consulta presença");

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Consulta Presença", Sexo.MASCULINO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        // Antes de marcar: 404 (é isto que o front usa pra decidir mostrar o
        // botão "Marcar presença" — sem isto ele não tem como saber que já
        // tinha marcado numa sessão anterior, e mostrava o botão do mesmo jeito).
        mockMvc.perform(get(BASE + "/lessons/" + lessonId + "/attendance/me").with(asUsuario(studentUser)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        mockMvc.perform(get(BASE + "/lessons/" + lessonId + "/attendance/me").with(asUsuario(studentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.present").value(true))
                .andExpect(jsonPath("$.personId").value(student.getId()));
    }

    @Test
    void getSelfAttendance_withoutEnrollment_isNotFound() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo presença não matriculado");
        Long classId = createActiveClassAsAdmin("Turma presença não matriculado", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula sem matrícula 2");
        publishLessonAsAdmin(lessonId, "Aula sem matrícula 2");

        Usuario outsiderUser = usuarioRepository.insert(UsuarioFixture.builder().build());

        mockMvc.perform(get(BASE + "/lessons/" + lessonId + "/attendance/me").with(asUsuario(outsiderUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAttendance_onDraftLesson_isRejected() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo presença 2");
        Long classId = createActiveClassAsAdmin("Turma presença 2", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula rascunho presença");

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Presença Rascunho", Sexo.MASCULINO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance").with(asUsuario(studentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void markAttendance_withoutEnrollment_isRejected() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo presença 3");
        Long classId = createActiveClassAsAdmin("Turma presença 3", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula sem matrícula");
        publishLessonAsAdmin(lessonId, "Aula sem matrícula");

        Pessoa outsider = PessoaFixture.membroComungante(pessoaService, "Não Matriculado", Sexo.FEMININO);
        Usuario outsiderUser = usuarioRepository.insert(UsuarioFixture.builder().personId(outsider.getId()).build());

        mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance").with(asUsuario(outsiderUser)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateAttendance_asStaff_flipsPresent() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo presença 4");
        Long classId = createActiveClassAsAdmin("Turma presença 4", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula retificação");
        publishLessonAsAdmin(lessonId, "Aula retificação");

        Pessoa student = PessoaFixture.membroComungante(pessoaService, "Aluno Retificação", Sexo.MASCULINO);
        Usuario studentUser = usuarioRepository.insert(UsuarioFixture.builder().personId(student.getId()).build());
        mockMvc.perform(post(BASE + "/classes/" + classId + "/enrollments").with(asUsuario(studentUser)))
                .andExpect(status().isOk());
        String body = mockMvc.perform(post(BASE + "/lessons/" + lessonId + "/attendance").with(asUsuario(studentUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long attendanceId = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(patch(BASE + "/attendance/" + attendanceId).with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"present\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.present").value(false));
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void listAttendance_isForbiddenForNonStaff() throws Exception {
        Long cycleId = createCycleAsAdmin("Ciclo presença forbidden");
        Long classId = createClassAsAdmin("Turma presença forbidden", cycleId);
        Long lessonId = createLessonAsAdmin(classId, "Aula forbidden");

        mockMvc.perform(get(BASE + "/lessons/" + lessonId + "/attendance"))
                .andExpect(status().isForbidden());
    }

    // ===== helpers =====

    private Long createLessonAsAdmin(Long classId, String title) throws Exception {
        String body = mockMvc.perform(post(BASE + "/classes/" + classId + "/lessons").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"," + LESSON_DATE + "}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void publishLessonAsAdmin(Long lessonId, String title) throws Exception {
        mockMvc.perform(put(BASE + "/lessons/" + lessonId).with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"," + LESSON_DATE + ",\"status\":\"PUBLISHED\"}"))
                .andExpect(status().isOk());
    }

    private Long createCycleAsAdmin(String name) throws Exception {
        String body = mockMvc.perform(post(BASE + "/cycles").with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }


    private Long createClassAsAdmin(String name, Long cycleId) throws Exception {
        String body = mockMvc.perform(post(BASE + "/classes").with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"cycleId\":" + cycleId + "}"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createActiveClassAsAdmin(String name, Long cycleId) throws Exception {
        Long classId = createClassAsAdmin(name, cycleId);
        mockMvc.perform(put(BASE + "/classes/" + classId).with(asAdmin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"cycleId\":" + cycleId + ",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
        return classId;
    }

    private RequestPostProcessor asAdmin() {
        Usuario admin = usuarioRepository.insert(UsuarioFixture.builder().accessProfile(PerfilAcesso.ADMIN).build());
        return asUsuario(admin);
    }

    private RequestPostProcessor asUsuario(Usuario usuario) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                new AuthUser(usuario.getId(), usuario.getEmail(), usuario.getAccessProfile()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getAccessProfile().name())));
        return authentication(authentication);
    }
}
