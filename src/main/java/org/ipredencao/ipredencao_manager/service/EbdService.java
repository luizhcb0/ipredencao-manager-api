package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.config.EbdAccess;
import org.ipredencao.ipredencao_manager.config.Roles;
import org.ipredencao.ipredencao_manager.controller.form.EbdClassForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdCycleForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdEnrollmentForm;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClass;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassStatusEnum;
import org.ipredencao.ipredencao_manager.model.ebd.EbdCycle;
import org.ipredencao.ipredencao_manager.model.ebd.EbdCycleQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollment;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollmentQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollmentRoleEnum;
import org.ipredencao.ipredencao_manager.model.pagination.PageInfo;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.repository.EbdClassRepository;
import org.ipredencao.ipredencao_manager.repository.EbdCycleRepository;
import org.ipredencao.ipredencao_manager.repository.EbdEnrollmentRepository;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
