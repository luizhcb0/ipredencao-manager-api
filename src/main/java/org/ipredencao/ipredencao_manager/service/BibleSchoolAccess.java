package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.config.Roles;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClass;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClassQuery;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClassStatusEnum;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolEnrollment;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolEnrollmentQuery;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolEnrollmentRoleEnum;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolLesson;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolLessonStatusEnum;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.repository.bible_school.BibleSchoolClassRepository;
import org.ipredencao.ipredencao_manager.repository.bible_school.BibleSchoolEnrollmentRepository;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;

// Quem pode o quê na Escola Dominical. O service chama daqui — @PreAuthorize
// não enxerga o body nem o hop lessonId/materialId → classId.
@Component
public class BibleSchoolAccess {

    @Autowired
    private UserService userService;
    @Autowired
    private BibleSchoolEnrollmentRepository enrollmentRepo;
    @Autowired
    private BibleSchoolClassRepository classRepo;

    public boolean isStaff() {
        return SecurityUtils.hasAnyRole(Roles.staffNames());
    }

    public boolean isTeacherOf(Long classId) {
        Long personId = currentPersonId();
        return personId != null && !enrollmentRepo.find(BibleSchoolEnrollmentQuery.builder()
                .classId(classId).personId(personId)
                .role(BibleSchoolEnrollmentRoleEnum.TEACHER).build()).isEmpty();
    }

    public boolean canManageClass(Long classId) {
        return isStaff() || isTeacherOf(classId);
    }

    // STAFF e professor vêem qualquer status; o resto só ACTIVE. 404, não 403
    // — mesma régua do detalhe (categoria 30).
    public BibleSchoolClass requireVisibleClass(Long classId) {
        BibleSchoolClass bibleSchoolClass = classRepo.find(BibleSchoolClassQuery.builder().id(classId).build())
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Turma " + classId + " não encontrada"));
        if (canManageClass(classId)) return bibleSchoolClass;
        if (bibleSchoolClass.status() == BibleSchoolClassStatusEnum.ACTIVE) return bibleSchoolClass;
        throw new NoSuchElementException("Turma " + classId + " não encontrada");
    }

    public Long currentPersonId() {
        return userService.currentPersonId();
    }

    public Long requireCurrentPersonId() {
        Long personId = currentPersonId();
        if (personId == null) {
            throw new IllegalStateException(
                    "Seu usuário ainda não está vinculado a um cadastro de pessoa; "
                    + "peça para um administrador vincular seu usuário antes de continuar");
        }
        return personId;
    }

    public void requireStaff() {
        if (!isStaff()) {
            throw new AccessDeniedException("Apenas um administrador pode fazer isso");
        }
    }

    public void requireStaffOrTeacher(Long classId) {
        if (!canManageClass(classId)) {
            throw new AccessDeniedException("Apenas um administrador ou o professor desta turma pode fazer isso");
        }
    }

    public void requireEnrolledStudent(Long classId) {
        Long personId = currentPersonId();
        boolean enrolled = personId != null
                && !enrollmentRepo.find(BibleSchoolEnrollmentQuery.builder().classId(classId).personId(personId)
                        .role(BibleSchoolEnrollmentRoleEnum.STUDENT).build()).isEmpty();
        if (!enrolled) {
            throw new AccessDeniedException("Apenas quem está matriculado nesta turma pode ver este conteúdo");
        }
    }

    public void requireCanRemoveEnrollment(Long classId, BibleSchoolEnrollment enrollment) {
        if (isStaff()) return;
        if (isTeacherOf(classId) && enrollment.role() == BibleSchoolEnrollmentRoleEnum.STUDENT) return;
        Long personId = requireCurrentPersonId();
        boolean ownStudent = enrollment.role() == BibleSchoolEnrollmentRoleEnum.STUDENT
                && enrollment.personId().equals(personId);
        if (!ownStudent) {
            throw new AccessDeniedException(
                    "Apenas o próprio aluno, o professor da turma ou um administrador pode remover esta matrícula");
        }
    }

    public void requireCanMutateAttendance(Long classId, Long attendancePersonId) {
        if (canManageClass(classId)) return;
        Long personId = currentPersonId();
        if (personId == null || !personId.equals(attendancePersonId)) {
            throw new AccessDeniedException(
                    "Apenas o próprio aluno, o professor da turma ou um administrador pode fazer isso");
        }
    }

    public void requirePublished(BibleSchoolLesson lesson) {
        if (lesson.status() != BibleSchoolLessonStatusEnum.PUBLISHED) {
            throw new AccessDeniedException("Esta aula ainda não foi publicada");
        }
    }

    public BibleSchoolClassQuery scopeClassQuery(BibleSchoolClassQuery query) {
        PaginationParameters pagination = query != null && query.pagination() != null
                ? query.pagination() : new PaginationParameters();
        pagination.applyDefaults();
        BibleSchoolClassQuery.Builder scoped = BibleSchoolClassQuery.builder()
                .id(query != null ? query.id() : null)
                .name(query != null ? query.name() : null)
                .cycleId(query != null ? query.cycleId() : null)
                .pagination(pagination);
        if (isStaff()) {
            return scoped
                    .ids(query != null ? query.ids() : null)
                    .status(query != null ? query.status() : null)
                    .build();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>(taughtClassIds());
        classRepo.find(BibleSchoolClassQuery.builder().status(BibleSchoolClassStatusEnum.ACTIVE).build())
                .forEach(c -> ids.add(c.id()));
        return scoped.ids(List.copyOf(ids)).build();
    }

    private List<Long> taughtClassIds() {
        Long personId = currentPersonId();
        if (personId == null) return List.of();
        return enrollmentRepo.find(BibleSchoolEnrollmentQuery.builder()
                        .personId(personId).role(BibleSchoolEnrollmentRoleEnum.TEACHER).build())
                .stream().map(BibleSchoolEnrollment::classId).toList();
    }
}
