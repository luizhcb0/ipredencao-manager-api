package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.config.EbdAccess;
import org.ipredencao.ipredencao_manager.config.Roles;
import org.ipredencao.ipredencao_manager.controller.form.EbdAttendanceForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdClassForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdCycleForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdEnrollmentForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdLessonForm;
import org.ipredencao.ipredencao_manager.model.ebd.EbdAttendance;
import org.ipredencao.ipredencao_manager.model.ebd.EbdAttendanceQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClass;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassStatusEnum;
import org.ipredencao.ipredencao_manager.model.ebd.EbdCycle;
import org.ipredencao.ipredencao_manager.model.ebd.EbdCycleQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollment;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollmentQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollmentRoleEnum;
import org.ipredencao.ipredencao_manager.model.ebd.EbdLesson;
import org.ipredencao.ipredencao_manager.model.ebd.EbdLessonQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdLessonStatusEnum;
import org.ipredencao.ipredencao_manager.model.ebd.EbdMaterial;
import org.ipredencao.ipredencao_manager.model.ebd.EbdMaterialContent;
import org.ipredencao.ipredencao_manager.model.ebd.EbdMaterialQuery;
import org.ipredencao.ipredencao_manager.model.pagination.PageInfo;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.repository.EbdAttendanceRepository;
import org.ipredencao.ipredencao_manager.repository.EbdClassRepository;
import org.ipredencao.ipredencao_manager.repository.EbdCycleRepository;
import org.ipredencao.ipredencao_manager.repository.EbdEnrollmentRepository;
import org.ipredencao.ipredencao_manager.repository.EbdLessonRepository;
import org.ipredencao.ipredencao_manager.repository.EbdMaterialRepository;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Service
public class EbdService {

    @Autowired
    private EbdCycleRepository cycleRepo;
    @Autowired
    private EbdClassRepository classRepo;
    @Autowired
    private EbdEnrollmentRepository enrollmentRepo;
    @Autowired
    private EbdLessonRepository lessonRepo;
    @Autowired
    private EbdMaterialRepository materialRepo;
    @Autowired
    private EbdAttendanceRepository attendanceRepo;
    @Autowired
    private EbdAccess ebdAccess;
    @Autowired
    private PessoaService pessoaService;
    @Autowired
    private SecurityUtils securityUtils;

    // ===== Ciclo =====

    @Transactional(readOnly = true)
    public List<EbdCycle> listCycles() {
        return cycleRepo.find(EbdCycleQuery.builder().build());
    }

    @Transactional
    public EbdCycle createCycle(EbdCycleForm form) {
        validateCycleName(form);
        boolean active = form.active() != null && form.active();
        assertSingleActiveCycle(active, null);
        if (cycleRepo.existsByName(form.name(), null)) {
            throw new IllegalArgumentException("Já existe um ciclo com o nome \"" + form.name() + "\"");
        }
        Long userId = securityUtils.getCurrentUserId();
        return cycleRepo.insert(form.name(), form.startDate(), form.endDate(), active, userId);
    }

    @Transactional
    public EbdCycle updateCycle(Long id, EbdCycleForm form) {
        EbdCycle existing = requireCycle(id);
        validateCycleName(form);
        boolean active = form.active() != null ? form.active() : existing.active();
        assertSingleActiveCycle(active, id);
        if (cycleRepo.existsByName(form.name(), id)) {
            throw new IllegalArgumentException("Já existe um ciclo com o nome \"" + form.name() + "\"");
        }
        Long userId = securityUtils.getCurrentUserId();
        cycleRepo.update(id, form.name(), form.startDate(), form.endDate(), active, userId);
        return requireCycle(id);
    }

    // ===== Turma =====

    @Transactional(readOnly = true)
    public PagedResponse<EbdClass> searchClasses(EbdClassQuery query) {
        PaginationParameters pagination = query.pagination() != null ? query.pagination() : new PaginationParameters();
        pagination.applyDefaults();
        // Visibilidade: STAFF vê qualquer status; qualquer outro autenticado
        // (incluindo professor e aluno) só enxerga turmas ACTIVE na busca geral —
        // "minhas turmas" (rascunho incluso, se professor) é resolvido por
        // matrícula, não por esta busca (ver docs/EBD_ANALISE_E_PLANO.md G).
        EbdClassStatusEnum status = isStaff() ? query.status() : EbdClassStatusEnum.ACTIVE;
        EbdClassQuery effectiveQuery = EbdClassQuery.builder()
                .id(query.id())
                .name(query.name())
                .fixed(query.fixed())
                .cycleId(query.cycleId())
                .status(status)
                .pagination(pagination)
                .build();
        List<EbdClass> classes = classRepo.find(effectiveQuery);
        int total = classRepo.count(effectiveQuery);
        return new PagedResponse<>(classes, new PageInfo(pagination.getLimit(), pagination.getOffset(), total));
    }

    @Transactional(readOnly = true)
    public EbdClass getClassDetail(Long id) {
        EbdClass base = requireClass(id);
        if (isStaff() || isTeacherOfClass(id)) {
            return base.withEnrollments(enrollmentRepo.find(EbdEnrollmentQuery.builder().classId(id).build()));
        }
        // Sem visibilidade administrativa: turma em rascunho/encerrada não pode
        // nem confirmar que existe (mesmo padrão da categoria 30 — 404, não 403).
        if (base.status() != EbdClassStatusEnum.ACTIVE) {
            throw new NoSuchElementException("Turma " + id + " não encontrada");
        }
        return base;
    }

    @Transactional
    public EbdClass createClass(EbdClassForm form) {
        validateClassName(form);
        if (form.fixed() == null) {
            throw new IllegalArgumentException("Campo obrigatório: fixed (turma fixa ou não-fixa)");
        }
        boolean fixed = form.fixed();
        validateCycleRequirement(fixed, form.cycleId());
        Long userId = securityUtils.getCurrentUserId();
        // Sempre nasce DRAFT — activar exige passar pelo fluxo de update (e, se
        // fixa, ter professor vinculado; ver requireTeacherForActivation).
        return classRepo.insert(form.cycleId(), fixed, form.name(), form.description(), form.syllabus(),
                EbdClassStatusEnum.DRAFT, userId);
    }

    @Transactional
    public EbdClass updateClass(Long id, EbdClassForm form) {
        EbdClass existing = requireClass(id);
        validateClassName(form);
        boolean fixed = form.fixed() != null ? form.fixed() : existing.fixed();
        Long cycleId = form.cycleId() != null ? form.cycleId() : existing.cycleId();
        if (!isStaff() && (!Objects.equals(fixed, existing.fixed()) || !Objects.equals(cycleId, existing.cycleId()))) {
            throw new AccessDeniedException("Apenas um administrador pode alterar o tipo da turma ou o ciclo");
        }
        validateCycleRequirement(fixed, cycleId);
        EbdClassStatusEnum status = form.status() != null ? form.status() : existing.status();
        if (fixed && status == EbdClassStatusEnum.ACTIVE) {
            requireTeacherForActivation(id);
        }
        Long userId = securityUtils.getCurrentUserId();
        classRepo.update(id, cycleId, fixed, form.name(), form.description(), form.syllabus(), status, userId);
        return requireClass(id);
    }

    // ===== Matrícula (administrativa: STAFF ou professor-da-turma) =====

    @Transactional(readOnly = true)
    public List<EbdEnrollment> listEnrollments(Long classId) {
        requireClass(classId);
        return enrollmentRepo.find(EbdEnrollmentQuery.builder().classId(classId).build());
    }

    @Transactional
    public EbdEnrollment addEnrollment(Long classId, EbdEnrollmentForm form) {
        EbdClass klass = requireClass(classId);
        if (form.personId() == null) {
            throw new IllegalArgumentException("Pessoa (personId) é obrigatória");
        }
        EbdEnrollmentRoleEnum role = form.role() != null ? form.role() : EbdEnrollmentRoleEnum.STUDENT;
        if (role == EbdEnrollmentRoleEnum.TEACHER) {
            if (!isStaff()) {
                throw new AccessDeniedException("Apenas um administrador pode associar professor a uma turma");
            }
        } else {
            if (!klass.fixed()) {
                throw new IllegalArgumentException(
                        "Turma não-fixa: o aluno se matricula sozinho (POST /api/ebd/classes/{id}/enrollments/me)");
            }
            if (!isStaff() && !isTeacherOfClass(classId)) {
                throw new AccessDeniedException("Apenas o professor da turma ou um administrador pode incluir aluno");
            }
        }
        pessoaService.findById(form.personId());
        if (enrollmentRepo.exists(classId, form.personId(), role)) {
            throw new IllegalArgumentException("Esta pessoa já está vinculada a esta turma com este papel");
        }
        Long userId = securityUtils.getCurrentUserId();
        Long id = enrollmentRepo.insert(classId, form.personId(), role, form.startDate(), userId);
        return requireEnrollment(id);
    }

    @Transactional
    public void removeEnrollment(Long classId, Long enrollmentId) {
        EbdEnrollment enrollment = requireEnrollment(enrollmentId);
        if (!enrollment.classId().equals(classId)) {
            throw new NoSuchElementException("Vínculo " + enrollmentId + " não encontrado nesta turma");
        }
        if (enrollment.role() == EbdEnrollmentRoleEnum.TEACHER) {
            if (!isStaff()) {
                throw new AccessDeniedException("Apenas um administrador pode remover um professor da turma");
            }
        } else if (!isStaff() && !isTeacherOfClass(classId)) {
            throw new AccessDeniedException("Apenas o professor da turma ou um administrador pode remover aluno");
        }
        enrollmentRepo.delete(enrollmentId);
    }

    // ===== Matrícula (automatrícula — turma não-fixa) =====

    @Transactional
    public EbdEnrollment selfEnroll(Long classId) {
        EbdClass klass = requireClass(classId);
        if (klass.fixed()) {
            throw new IllegalArgumentException(
                    "Turma fixa: a matrícula é feita pelo professor ou por um administrador");
        }
        if (klass.status() != EbdClassStatusEnum.ACTIVE) {
            throw new IllegalStateException("Só é possível se matricular em uma turma ativa");
        }
        Long personId = requireCurrentPersonId();
        if (enrollmentRepo.exists(classId, personId, EbdEnrollmentRoleEnum.STUDENT)) {
            throw new IllegalArgumentException("Você já está matriculado nesta turma");
        }
        Long userId = securityUtils.getCurrentUserId();
        Long id = enrollmentRepo.insert(classId, personId, EbdEnrollmentRoleEnum.STUDENT, null, userId);
        return requireEnrollment(id);
    }

    @Transactional
    public void selfUnenroll(Long classId) {
        EbdClass klass = requireClass(classId);
        if (klass.fixed()) {
            throw new IllegalArgumentException(
                    "Turma fixa: o cancelamento é feito pelo professor ou por um administrador");
        }
        Long personId = requireCurrentPersonId();
        EbdEnrollment enrollment = enrollmentRepo
                .find(EbdEnrollmentQuery.builder().classId(classId).personId(personId)
                        .role(EbdEnrollmentRoleEnum.STUDENT).build())
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Você não está matriculado nesta turma"));
        enrollmentRepo.delete(enrollment.id());
    }

    // ===== Aula =====

    @Transactional(readOnly = true)
    public List<EbdLesson> listLessons(Long classId) {
        requireClass(classId);
        if (isStaff() || isTeacherOfClass(classId)) {
            return lessonRepo.find(EbdLessonQuery.builder().classId(classId).build());
        }
        requireEnrolledStudent(classId);
        return lessonRepo.find(EbdLessonQuery.builder().classId(classId).status(EbdLessonStatusEnum.PUBLISHED).build());
    }

    @Transactional
    public EbdLesson createLesson(Long classId, EbdLessonForm form) {
        requireClass(classId);
        validateLessonTitle(form);
        int order = form.displayOrder() != null ? form.displayOrder() : lessonRepo.countByClass(classId);
        Long userId = securityUtils.getCurrentUserId();
        // Sempre nasce DRAFT, mesmo padrão de ebd_class (ver createClass).
        Long id = lessonRepo.insert(classId, form.title(), form.description(), form.content(), form.lessonDate(),
                order, EbdLessonStatusEnum.DRAFT, userId);
        return requireLesson(id);
    }

    @Transactional
    public EbdLesson updateLesson(Long lessonId, EbdLessonForm form) {
        EbdLesson existing = requireLesson(lessonId);
        requireStaffOrTeacher(existing.classId());
        validateLessonTitle(form);
        int order = form.displayOrder() != null ? form.displayOrder() : existing.displayOrder();
        EbdLessonStatusEnum status = form.status() != null ? form.status() : existing.status();
        Long userId = securityUtils.getCurrentUserId();
        lessonRepo.update(lessonId, existing.classId(), form.title(), form.description(), form.content(),
                form.lessonDate(), order, status, userId);
        return requireLesson(lessonId);
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        EbdLesson existing = requireLesson(lessonId);
        requireStaffOrTeacher(existing.classId());
        lessonRepo.delete(lessonId); // cascade apaga materiais da aula (V016)
    }

    // ===== Material =====

    @Transactional(readOnly = true)
    public List<EbdMaterial> listClassMaterials(Long classId) {
        requireClass(classId);
        if (!isStaff() && !isTeacherOfClass(classId)) {
            requireEnrolledStudent(classId);
        }
        return materialRepo.find(EbdMaterialQuery.builder().classId(classId).generalOnly(true).build());
    }

    @Transactional(readOnly = true)
    public List<EbdMaterial> listLessonMaterials(Long lessonId) {
        EbdLesson lesson = requireLesson(lessonId);
        if (!isStaff() && !isTeacherOfClass(lesson.classId())) {
            requireEnrolledStudent(lesson.classId());
            requirePublished(lesson);
        }
        return materialRepo.find(EbdMaterialQuery.builder().lessonId(lessonId).build());
    }

    @Transactional
    public EbdMaterial addClassMaterial(Long classId, MultipartFile file) {
        requireClass(classId);
        requireStaffOrTeacher(classId);
        return insertMaterial(classId, null, file);
    }

    @Transactional
    public EbdMaterial addLessonMaterial(Long lessonId, MultipartFile file) {
        EbdLesson lesson = requireLesson(lessonId);
        requireStaffOrTeacher(lesson.classId());
        return insertMaterial(lesson.classId(), lessonId, file);
    }

    @Transactional
    public void deleteMaterial(Long materialId) {
        EbdMaterial material = requireMaterial(materialId);
        requireStaffOrTeacher(material.classId());
        materialRepo.delete(materialId);
    }

    // Visibilidade espelha listLessonMaterials/listClassMaterials: STAFF/professor
    // sempre; aluno matriculado só se o material for geral ou da aula publicada.
    @Transactional(readOnly = true)
    public EbdMaterialContent downloadMaterial(Long materialId) {
        EbdMaterial material = requireMaterial(materialId);
        if (!isStaff() && !isTeacherOfClass(material.classId())) {
            requireEnrolledStudent(material.classId());
            if (material.lessonId() != null) {
                requirePublished(requireLesson(material.lessonId()));
            }
        }
        return materialRepo.findContent(materialId);
    }

    private EbdMaterial insertMaterial(Long classId, Long lessonId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo é obrigatório");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Erro ao ler o arquivo enviado", e);
        }
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "arquivo";
        Long userId = securityUtils.getCurrentUserId();
        Long id = materialRepo.insert(classId, lessonId, fileName, file.getContentType(), file.getSize(), data, userId);
        return requireMaterial(id);
    }

    // ===== Presença =====

    // Autodeclarada pelo aluno já matriculado, nos dois tipos de turma — a
    // diferença fixa/não-fixa já foi resolvida na matrícula (ebd_enrollment),
    // presença não repete essa checagem.
    @Transactional
    public EbdAttendance selfReportAttendance(Long lessonId) {
        EbdLesson lesson = requireLesson(lessonId);
        requirePublished(lesson);
        Long personId = requireCurrentPersonId();
        EbdEnrollment enrollment = enrollmentRepo
                .find(EbdEnrollmentQuery.builder().classId(lesson.classId()).personId(personId)
                        .role(EbdEnrollmentRoleEnum.STUDENT).build())
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Você não está matriculado nesta turma"));
        if (attendanceRepo.exists(lessonId, enrollment.id())) {
            throw new IllegalArgumentException("Você já registrou presença nesta aula");
        }
        Long userId = securityUtils.getCurrentUserId();
        Long id = attendanceRepo.insert(lessonId, enrollment.id(), true, true, userId);
        return requireAttendance(id);
    }

    // Consulta da própria presença — sem isto o aluno não tinha como saber, ao
    // carregar a lista de aulas, se já tinha marcado presença numa sessão
    // anterior (só descobria tentando marcar de novo e recebendo o 400 de
    // duplicidade). 404 cobre "nunca marcou", "não está matriculado" e "sem
    // usuario.person_id resolvido" da mesma forma — o chamador só precisa
    // saber "existe ou não", não por quê.
    @Transactional(readOnly = true)
    public EbdAttendance getSelfAttendance(Long lessonId) {
        EbdLesson lesson = requireLesson(lessonId);
        Long personId = ebdAccess.currentPersonId(SecurityContextHolder.getContext().getAuthentication());
        if (personId == null) {
            throw new NoSuchElementException("Presença não encontrada");
        }
        EbdEnrollment enrollment = enrollmentRepo
                .find(EbdEnrollmentQuery.builder().classId(lesson.classId()).personId(personId)
                        .role(EbdEnrollmentRoleEnum.STUDENT).build())
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Presença não encontrada"));
        return attendanceRepo
                .find(EbdAttendanceQuery.builder().lessonId(lessonId).enrollmentId(enrollment.id()).build())
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Presença não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<EbdAttendance> listAttendance(Long lessonId) {
        EbdLesson lesson = requireLesson(lessonId);
        requireStaffOrTeacher(lesson.classId());
        return attendanceRepo.find(EbdAttendanceQuery.builder().lessonId(lessonId).build());
    }

    // Retificação: STAFF/professor registrando ou corrigindo em nome do aluno
    // (self_reported vira false, mesmo quando o valor de present não muda).
    @Transactional
    public EbdAttendance rectifyAttendance(Long attendanceId, EbdAttendanceForm form) {
        EbdAttendance existing = requireAttendance(attendanceId);
        EbdLesson lesson = requireLesson(existing.lessonId());
        requireStaffOrTeacher(lesson.classId());
        if (form == null || form.present() == null) {
            throw new IllegalArgumentException("Campo obrigatório: present");
        }
        Long userId = securityUtils.getCurrentUserId();
        attendanceRepo.update(attendanceId, form.present(), false, userId);
        return requireAttendance(attendanceId);
    }

    // ===== Validações / helpers =====

    private EbdCycle requireCycle(Long id) {
        return cycleRepo.find(EbdCycleQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Ciclo " + id + " não encontrado"));
    }

    private EbdClass requireClass(Long id) {
        return classRepo.find(EbdClassQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Turma " + id + " não encontrada"));
    }

    private EbdEnrollment requireEnrollment(Long id) {
        return enrollmentRepo.find(EbdEnrollmentQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Vínculo " + id + " não encontrado"));
    }

    private EbdLesson requireLesson(Long id) {
        return lessonRepo.find(EbdLessonQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Aula " + id + " não encontrada"));
    }

    private EbdMaterial requireMaterial(Long id) {
        return materialRepo.find(EbdMaterialQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Material " + id + " não encontrado"));
    }

    private EbdAttendance requireAttendance(Long id) {
        return attendanceRepo.find(EbdAttendanceQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Presença " + id + " não encontrada"));
    }

    private void validateLessonTitle(EbdLessonForm form) {
        if (form == null || form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Título da aula é obrigatório");
        }
    }

    // Reutilizado por aula e material: quem edita uma delas precisa ser STAFF ou
    // o professor da turma dona (não dá para expressar em @PreAuthorize porque
    // o path variable desses endpoints é o id da aula/material, não da turma).
    private void requireStaffOrTeacher(Long classId) {
        if (!isStaff() && !isTeacherOfClass(classId)) {
            throw new AccessDeniedException("Apenas o professor da turma ou um administrador pode fazer isso");
        }
    }

    private void requireEnrolledStudent(Long classId) {
        Long personId = ebdAccess.currentPersonId(SecurityContextHolder.getContext().getAuthentication());
        boolean enrolled = personId != null && enrollmentRepo.exists(classId, personId, EbdEnrollmentRoleEnum.STUDENT);
        if (!enrolled) {
            throw new AccessDeniedException("Apenas quem está matriculado nesta turma pode ver este conteúdo");
        }
    }

    private void requirePublished(EbdLesson lesson) {
        if (lesson.status() != EbdLessonStatusEnum.PUBLISHED) {
            throw new AccessDeniedException("Esta aula ainda não foi publicada");
        }
    }

    private void validateCycleName(EbdCycleForm form) {
        if (form == null || form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("Nome do ciclo é obrigatório");
        }
    }

    private void validateClassName(EbdClassForm form) {
        if (form == null || form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("Nome da turma é obrigatório");
        }
    }

    // Não-fixa sempre pertence a um ciclo; fixa pode ou não (ver V015). Quando
    // um cycleId é informado (em qualquer um dos dois casos) ele precisa existir.
    private void validateCycleRequirement(boolean fixed, Long cycleId) {
        if (!fixed && cycleId == null) {
            throw new IllegalArgumentException("Turma não-fixa precisa de um ciclo (cycleId)");
        }
        if (cycleId != null) {
            requireCycle(cycleId);
        }
    }

    // Espelha o índice único parcial de V015 (uq_ebd_cycle_single_active) para
    // devolver 400 em PT em vez de deixar a violação de constraint virar 500.
    private void assertSingleActiveCycle(boolean active, Long excludeId) {
        if (active && cycleRepo.existsOtherActive(excludeId)) {
            throw new IllegalArgumentException("Já existe um ciclo ativo; desative-o antes de ativar outro");
        }
    }

    // Decisão confirmada com o usuário: turma fixa não pode ser ativada sem
    // professor vinculado.
    private void requireTeacherForActivation(Long classId) {
        boolean hasTeacher = !enrollmentRepo.find(EbdEnrollmentQuery.builder()
                .classId(classId).role(EbdEnrollmentRoleEnum.TEACHER).build()).isEmpty();
        if (!hasTeacher) {
            throw new IllegalStateException("Turma fixa não pode ser ativada sem professor vinculado");
        }
    }

    private boolean isStaff() {
        return SecurityUtils.hasAnyRole(Roles.staffNames());
    }

    private boolean isTeacherOfClass(Long classId) {
        return ebdAccess.isTeacherOf(SecurityContextHolder.getContext().getAuthentication(), classId);
    }

    // Usado pelas ações de autoatendimento (matricular-se/cancelar). O vínculo
    // usuario.person_id em si é gerenciado fora da EBD (V014) — aqui só se exige
    // que ele já esteja resolvido.
    private Long requireCurrentPersonId() {
        Long personId = ebdAccess.currentPersonId(SecurityContextHolder.getContext().getAuthentication());
        if (personId == null) {
            throw new IllegalStateException(
                    "Seu usuário ainda não está vinculado a um cadastro de pessoa; "
                    + "peça para um administrador vincular seu usuário antes de continuar");
        }
        return personId;
    }
}
