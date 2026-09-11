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
        assertUniqueCycleName(form.name(), null);
        Long userId = securityUtils.getCurrentUserId();
        return cycleRepo.insert(form.name(), form.startDate(), form.endDate(), active, userId);
    }

    @Transactional
    public EbdCycle updateCycle(Long id, EbdCycleForm form) {
        EbdCycle existing = requireCycle(id);
        validateCycleName(form);
        boolean active = form.active() != null ? form.active() : existing.active();
        assertSingleActiveCycle(active, id);
        assertUniqueCycleName(form.name(), id);
        Long userId = securityUtils.getCurrentUserId();
        cycleRepo.update(id, form.name(), form.startDate(), form.endDate(), active, userId);
        return requireCycle(id);
    }

    // ===== Turma =====

    @Transactional(readOnly = true)
    public PagedResponse<EbdClass> searchClasses(EbdClassQuery query) {
        PaginationParameters pagination = query.pagination() != null ? query.pagination() : new PaginationParameters();
        pagination.applyDefaults();
        // Visibilidade: STAFF vê qualquer status; qualquer outro autenticado só
        // enxerga turmas ACTIVE na busca geral (ver docs/EBD_ANALISE_E_PLANO.md G).
        EbdClassStatusEnum status = isStaff() ? query.status() : EbdClassStatusEnum.ACTIVE;
        EbdClassQuery effectiveQuery = EbdClassQuery.builder()
                .id(query.id())
                .name(query.name())
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
        if (isStaff()) {
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
        requireCycle(requireCycleId(form.cycleId()));
        Long userId = securityUtils.getCurrentUserId();
        // Sempre nasce DRAFT — activar exige passar pelo fluxo de update.
        return classRepo.insert(form.cycleId(), form.name(), form.description(), form.syllabus(),
                EbdClassStatusEnum.DRAFT, userId);
    }

    @Transactional
    public EbdClass updateClass(Long id, EbdClassForm form) {
        EbdClass existing = requireClass(id);
        validateClassName(form);
        Long cycleId = form.cycleId() != null ? form.cycleId() : existing.cycleId();
        requireCycle(cycleId);
        EbdClassStatusEnum status = form.status() != null ? form.status() : existing.status();
        Long userId = securityUtils.getCurrentUserId();
        classRepo.update(id, cycleId, form.name(), form.description(), form.syllabus(), status, userId);
        return requireClass(id);
    }

    // ===== Matrícula =====
    // Um único endpoint (POST/DELETE .../enrollments[/{id}]) serve tanto a
    // matrícula administrativa (STAFF, qualquer role) quanto a automatrícula
    // (qualquer autenticado com pessoa vinculada, sempre STUDENT) — a diferença
    // é decidida no service, não dá pra expressar "olhe o personId do body" em
    // @PreAuthorize. .../enrollments/me continua existindo só pro DELETE, como
    // atalho pra quem não sabe o próprio enrollmentId (resolve e delega pro
    // mesmo removeEnrollment).

    @Transactional(readOnly = true)
    public List<EbdEnrollment> listEnrollments(Long classId) {
        requireClass(classId);
        return enrollmentRepo.find(EbdEnrollmentQuery.builder().classId(classId).build());
    }

    @Transactional
    public EbdEnrollment addEnrollment(Long classId, EbdEnrollmentForm form) {
        EbdClass klass = requireClass(classId);
        Long personId;
        EbdEnrollmentRoleEnum role;
        String duplicateMessage;
        if (form != null && form.personId() != null) {
            requireStaff();
            pessoaService.findById(form.personId());
            personId = form.personId();
            role = form.role() != null ? form.role() : EbdEnrollmentRoleEnum.STUDENT;
            duplicateMessage = "Esta pessoa já está vinculada a esta turma com este papel";
        } else {
            if (klass.status() != EbdClassStatusEnum.ACTIVE) {
                throw new IllegalStateException("Só é possível se matricular em uma turma ativa");
            }
            personId = requireCurrentPersonId();
            role = EbdEnrollmentRoleEnum.STUDENT;
            duplicateMessage = "Você já está matriculado nesta turma";
        }
        if (!enrollmentRepo.find(EbdEnrollmentQuery.builder().classId(classId).personId(personId).role(role).build())
                .isEmpty()) {
            throw new IllegalArgumentException(duplicateMessage);
        }
        Long userId = securityUtils.getCurrentUserId();
        Long id = enrollmentRepo.insert(classId, personId, role, userId);
        return requireEnrollment(id);
    }

    @Transactional
    public void removeEnrollment(Long classId, Long enrollmentId) {
        EbdEnrollment enrollment = requireEnrollment(enrollmentId);
        if (!enrollment.classId().equals(classId)) {
            throw new NoSuchElementException("Vínculo " + enrollmentId + " não encontrado nesta turma");
        }
        if (!isStaff()) {
            Long personId = requireCurrentPersonId();
            boolean isOwnStudentEnrollment = enrollment.role() == EbdEnrollmentRoleEnum.STUDENT
                    && enrollment.personId().equals(personId);
            if (!isOwnStudentEnrollment) {
                throw new AccessDeniedException("Apenas o próprio aluno ou um administrador pode remover esta matrícula");
            }
        }
        enrollmentRepo.delete(enrollmentId);
    }

    @Transactional
    public void selfUnenroll(Long classId) {
        Long personId = requireCurrentPersonId();
        EbdEnrollment enrollment = enrollmentRepo
                .find(EbdEnrollmentQuery.builder().classId(classId).personId(personId)
                        .role(EbdEnrollmentRoleEnum.STUDENT).build())
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Você não está matriculado nesta turma"));
        removeEnrollment(classId, enrollment.id());
    }

    // ===== Aula =====

    @Transactional(readOnly = true)
    public List<EbdLesson> listLessons(Long classId) {
        requireClass(classId);
        if (isStaff()) {
            return lessonRepo.find(EbdLessonQuery.builder().classId(classId).build());
        }
        requireEnrolledStudent(classId);
        return lessonRepo.find(EbdLessonQuery.builder().classId(classId).status(EbdLessonStatusEnum.PUBLISHED).build());
    }

    @Transactional
    public EbdLesson createLesson(Long classId, EbdLessonForm form) {
        requireClass(classId);
        validateLessonForm(form);
        Long userId = securityUtils.getCurrentUserId();
        // Sempre nasce DRAFT, mesmo padrão de ebd_class (ver createClass).
        Long id = lessonRepo.insert(classId, form.title(), form.description(), form.lessonDate(),
                EbdLessonStatusEnum.DRAFT, userId);
        return requireLesson(id);
    }

    @Transactional
    public EbdLesson updateLesson(Long lessonId, EbdLessonForm form) {
        EbdLesson existing = requireLesson(lessonId);
        requireStaff();
        validateLessonForm(form);
        EbdLessonStatusEnum status = form.status() != null ? form.status() : existing.status();
        Long userId = securityUtils.getCurrentUserId();
        lessonRepo.update(lessonId, existing.classId(), form.title(), form.description(), form.lessonDate(),
                status, userId);
        return requireLesson(lessonId);
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        requireLesson(lessonId);
        requireStaff();
        lessonRepo.delete(lessonId); // cascade apaga materiais da aula (V015)
    }

    // ===== Material =====

    @Transactional(readOnly = true)
    public List<EbdMaterial> listClassMaterials(Long classId) {
        requireClass(classId);
        if (!isStaff()) {
            requireEnrolledStudent(classId);
        }
        return materialRepo.find(EbdMaterialQuery.builder().classId(classId).generalOnly(true).build());
    }

    @Transactional(readOnly = true)
    public List<EbdMaterial> listLessonMaterials(Long lessonId) {
        EbdLesson lesson = requireLesson(lessonId);
        if (!isStaff()) {
            requireEnrolledStudent(lesson.classId());
            requirePublished(lesson);
        }
        return materialRepo.find(EbdMaterialQuery.builder().lessonId(lessonId).build());
    }

    @Transactional
    public EbdMaterial addClassMaterial(Long classId, MultipartFile file) {
        requireClass(classId);
        return insertMaterial(classId, null, file);
    }

    @Transactional
    public EbdMaterial addLessonMaterial(Long lessonId, MultipartFile file) {
        EbdLesson lesson = requireLesson(lessonId);
        requireStaff();
        return insertMaterial(lesson.classId(), lessonId, file);
    }

    @Transactional
    public void deleteMaterial(Long materialId) {
        requireMaterial(materialId);
        requireStaff();
        materialRepo.delete(materialId);
    }

    // Visibilidade espelha listLessonMaterials/listClassMaterials: STAFF sempre;
    // aluno matriculado só se o material for geral ou da aula publicada.
    @Transactional(readOnly = true)
    public EbdMaterial downloadMaterial(Long materialId) {
        EbdMaterial material = requireMaterial(materialId);
        if (!isStaff()) {
            requireEnrolledStudent(material.classId());
            if (material.lessonId() != null) {
                requirePublished(requireLesson(material.lessonId()));
            }
        }
        return materialRepo.findWithData(materialId);
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
    // Um único par de endpoints (POST .../lessons/{id}/attendance cria, PATCH
    // .../attendance/{id} atualiza, DELETE .../attendance/{id} remove) serve
    // professor e aluno: personId no body (só STAFF pode informar) marca/altera
    // em nome de outra pessoa; sem personId, é sempre em nome de quem chama.

    @Transactional
    public EbdAttendance markAttendance(Long lessonId, EbdAttendanceForm form) {
        EbdLesson lesson = requireLesson(lessonId);
        requirePublished(lesson);
        Long personId;
        if (form != null && form.personId() != null) {
            requireStaff();
            personId = form.personId();
        } else {
            personId = requireCurrentPersonId();
        }
        EbdEnrollment enrollment = enrollmentRepo
                .find(EbdEnrollmentQuery.builder().classId(lesson.classId()).personId(personId)
                        .role(EbdEnrollmentRoleEnum.STUDENT).build())
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Esta pessoa não está matriculada nesta turma"));
        if (attendanceRepo.exists(lessonId, enrollment.id())) {
            throw new IllegalArgumentException("Presença já registrada nesta aula");
        }
        boolean present = form == null || form.present() == null || form.present();
        Long userId = securityUtils.getCurrentUserId();
        Long id = attendanceRepo.insert(lessonId, enrollment.id(), present, userId);
        return requireAttendance(id);
    }

    @Transactional
    public EbdAttendance updateAttendance(Long attendanceId, EbdAttendanceForm form) {
        EbdAttendance existing = requireAttendance(attendanceId);
        requireOwnAttendanceOrStaff(existing);
        if (form == null || form.present() == null) {
            throw new IllegalArgumentException("Campo obrigatório: present");
        }
        Long userId = securityUtils.getCurrentUserId();
        attendanceRepo.update(attendanceId, form.present(), userId);
        return requireAttendance(attendanceId);
    }

    @Transactional
    public void deleteAttendance(Long attendanceId) {
        EbdAttendance existing = requireAttendance(attendanceId);
        requireOwnAttendanceOrStaff(existing);
        attendanceRepo.delete(attendanceId);
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
        requireLesson(lessonId);
        requireStaff();
        return attendanceRepo.find(EbdAttendanceQuery.builder().lessonId(lessonId).build());
    }

    private void requireOwnAttendanceOrStaff(EbdAttendance attendance) {
        if (isStaff()) return;
        Long personId = ebdAccess.currentPersonId(SecurityContextHolder.getContext().getAuthentication());
        if (personId == null || !personId.equals(attendance.personId())) {
            throw new AccessDeniedException("Apenas o próprio aluno ou um administrador pode fazer isso");
        }
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

    private Long requireCycleId(Long cycleId) {
        if (cycleId == null) {
            throw new IllegalArgumentException("Campo obrigatório: cycleId (toda turma pertence a um ciclo)");
        }
        return cycleId;
    }

    private void validateLessonForm(EbdLessonForm form) {
        if (form == null || form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Título da aula é obrigatório");
        }
        if (form.lessonDate() == null) {
            throw new IllegalArgumentException("Data da aula é obrigatória");
        }
    }

    // Reutilizado pelos endpoints de aula/material cujo path variable é o id da
    // aula/material, não o da turma (não dá para expressar STAFF em
    // @PreAuthorize sem esse id) — ver comentário em EbdController.
    private void requireStaff() {
        if (!isStaff()) {
            throw new AccessDeniedException("Apenas um administrador pode fazer isso");
        }
    }

    private void requireEnrolledStudent(Long classId) {
        Long personId = ebdAccess.currentPersonId(SecurityContextHolder.getContext().getAuthentication());
        boolean enrolled = personId != null
                && !enrollmentRepo.find(EbdEnrollmentQuery.builder().classId(classId).personId(personId)
                        .role(EbdEnrollmentRoleEnum.STUDENT).build()).isEmpty();
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

    private void assertUniqueCycleName(String name, Long excludeId) {
        if (!cycleRepo.find(EbdCycleQuery.builder().name(name).excludeId(excludeId).build()).isEmpty()) {
            throw new IllegalArgumentException("Já existe um ciclo com o nome \"" + name + "\"");
        }
    }

    private void validateClassName(EbdClassForm form) {
        if (form == null || form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("Nome da turma é obrigatório");
        }
    }

    // Espelha o índice único parcial de V015 (uq_ebd_cycle_single_active) para
    // devolver 400 em PT em vez de deixar a violação de constraint virar 500.
    private void assertSingleActiveCycle(boolean active, Long excludeId) {
        if (active && !cycleRepo.find(EbdCycleQuery.builder().active(true).excludeId(excludeId).build()).isEmpty()) {
            throw new IllegalArgumentException("Já existe um ciclo ativo; desative-o antes de ativar outro");
        }
    }

    private boolean isStaff() {
        return SecurityUtils.hasAnyRole(Roles.staffNames());
    }

    // Usado pelas ações de autoatendimento (matricular-se/marcar presença/
    // cancelar). O vínculo usuario.person_id em si é gerenciado fora da EBD
    // (V014) — aqui só se exige que ele já esteja resolvido.
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
