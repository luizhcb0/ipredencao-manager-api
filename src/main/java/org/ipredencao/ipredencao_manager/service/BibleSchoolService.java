package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.controller.form.BibleSchoolAttendanceForm;
import org.ipredencao.ipredencao_manager.controller.form.BibleSchoolClassForm;
import org.ipredencao.ipredencao_manager.controller.form.BibleSchoolCycleForm;
import org.ipredencao.ipredencao_manager.controller.form.BibleSchoolEnrollmentForm;
import org.ipredencao.ipredencao_manager.controller.form.BibleSchoolLessonForm;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolAttendance;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolAttendanceQuery;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClass;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClassQuery;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClassStatusEnum;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolCycle;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolCycleQuery;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolEnrollment;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolEnrollmentQuery;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolEnrollmentRoleEnum;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolLesson;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolLessonQuery;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolLessonStatusEnum;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolMaterial;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolMaterialQuery;
import org.ipredencao.ipredencao_manager.model.pagination.PageInfo;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.repository.bible_school.BibleSchoolAttendanceRepository;
import org.ipredencao.ipredencao_manager.repository.bible_school.BibleSchoolClassRepository;
import org.ipredencao.ipredencao_manager.repository.bible_school.BibleSchoolCycleRepository;
import org.ipredencao.ipredencao_manager.repository.bible_school.BibleSchoolEnrollmentRepository;
import org.ipredencao.ipredencao_manager.repository.bible_school.BibleSchoolLessonRepository;
import org.ipredencao.ipredencao_manager.repository.bible_school.BibleSchoolMaterialRepository;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class BibleSchoolService {

    @Autowired
    private BibleSchoolCycleRepository cycleRepo;
    @Autowired
    private BibleSchoolClassRepository classRepo;
    @Autowired
    private BibleSchoolEnrollmentRepository enrollmentRepo;
    @Autowired
    private BibleSchoolLessonRepository lessonRepo;
    @Autowired
    private BibleSchoolMaterialRepository materialRepo;
    @Autowired
    private BibleSchoolAttendanceRepository attendanceRepo;
    @Autowired
    private PessoaService pessoaService;
    @Autowired
    private SecurityUtils securityUtils;
    @Autowired
    private BibleSchoolAccess access;

    @Transactional(readOnly = true)
    public List<BibleSchoolCycle> listCycles() {
        return cycleRepo.find(BibleSchoolCycleQuery.builder().build());
    }

    @Transactional
    public BibleSchoolCycle createCycle(BibleSchoolCycleForm form) {
        validateCycleName(form);
        boolean active = form.active() != null && form.active();
        assertSingleActiveCycle(active, null);
        assertUniqueCycleName(form.name(), null);
        return cycleRepo.insert(new BibleSchoolCycle(
                null, form.name(), form.startDate(), form.endDate(), active,
                null, null, securityUtils.getCurrentUserId()));
    }

    @Transactional
    public BibleSchoolCycle updateCycle(Long id, BibleSchoolCycleForm form) {
        BibleSchoolCycle existing = requireCycle(id);
        validateCycleName(form);
        boolean active = form.active() != null ? form.active() : existing.active();
        assertSingleActiveCycle(active, id);
        assertUniqueCycleName(form.name(), id);
        return cycleRepo.update(new BibleSchoolCycle(
                id, form.name(), form.startDate(), form.endDate(), active,
                null, null, securityUtils.getCurrentUserId()));
    }

    @Transactional(readOnly = true)
    public PagedResponse<BibleSchoolClass> searchClasses(BibleSchoolClassQuery query) {
        BibleSchoolClassQuery scoped = access.scopeClassQuery(query);
        PaginationParameters pagination = scoped.pagination();
        return new PagedResponse<>(
                classRepo.find(scoped),
                new PageInfo(pagination.getLimit(), pagination.getOffset(), classRepo.count(scoped)));
    }

    @Transactional(readOnly = true)
    public BibleSchoolClass getClassDetail(Long id) {
        BibleSchoolClass base = access.requireVisibleClass(id);
        if (access.canManageClass(id)) {
            return base.withEnrollments(enrollmentRepo.find(BibleSchoolEnrollmentQuery.builder().classId(id).build()));
        }
        return base;
    }

    @Transactional
    public BibleSchoolClass createClass(BibleSchoolClassForm form) {
        validateClassName(form);
        requireCycle(requireCycleId(form.cycleId()));
        return classRepo.insert(new BibleSchoolClass(
                null, form.cycleId(), null, form.name(), form.description(),
                BibleSchoolClassStatusEnum.DRAFT, null, null, securityUtils.getCurrentUserId(), null));
    }

    @Transactional
    public BibleSchoolClass updateClass(Long id, BibleSchoolClassForm form) {
        BibleSchoolClass existing = access.requireVisibleClass(id);
        access.requireStaffOrTeacher(id);
        validateClassName(form);
        Long cycleId = form.cycleId() != null ? form.cycleId() : existing.cycleId();
        requireCycle(cycleId);
        BibleSchoolClassStatusEnum status = form.status() != null ? form.status() : existing.status();
        return classRepo.update(new BibleSchoolClass(
                id, cycleId, null, form.name(), form.description(),
                status, null, null, securityUtils.getCurrentUserId(), null));
    }

    @Transactional(readOnly = true)
    public List<BibleSchoolEnrollment> listEnrollments(Long classId) {
        access.requireVisibleClass(classId);
        access.requireStaffOrTeacher(classId);
        return enrollmentRepo.find(BibleSchoolEnrollmentQuery.builder().classId(classId).build());
    }

    // personId no body = STAFF ou professor da turma; ausente = o próprio chamador. @PreAuthorize não enxerga o body.
    @Transactional
    public BibleSchoolEnrollment addEnrollment(Long classId, BibleSchoolEnrollmentForm form) {
        BibleSchoolClass bibleSchoolClass = access.requireVisibleClass(classId);
        Long personId;
        BibleSchoolEnrollmentRoleEnum role;
        String duplicateMessage;
        if (form != null && form.personId() != null) {
            access.requireStaffOrTeacher(classId);
            role = form.role() != null ? form.role() : BibleSchoolEnrollmentRoleEnum.STUDENT;
            if (role == BibleSchoolEnrollmentRoleEnum.TEACHER) {
                access.requireStaff();
            }
            pessoaService.findById(form.personId());
            personId = form.personId();
            duplicateMessage = "Esta pessoa já está vinculada a esta turma com este papel";
        } else {
            if (bibleSchoolClass.status() != BibleSchoolClassStatusEnum.ACTIVE) {
                throw new IllegalStateException("Só é possível se matricular em uma turma ativa");
            }
            personId = access.requireCurrentPersonId();
            role = BibleSchoolEnrollmentRoleEnum.STUDENT;
            duplicateMessage = "Você já está matriculado nesta turma";
        }
        if (!enrollmentRepo.find(BibleSchoolEnrollmentQuery.builder().classId(classId).personId(personId).role(role).build())
                .isEmpty()) {
            throw new IllegalArgumentException(duplicateMessage);
        }
        return enrollmentRepo.insert(new BibleSchoolEnrollment(
                null, classId, null, personId, null, role,
                null, null, securityUtils.getCurrentUserId()));
    }

    @Transactional
    public void removeEnrollment(Long classId, Long enrollmentId) {
        access.requireVisibleClass(classId);
        BibleSchoolEnrollment enrollment = requireEnrollment(enrollmentId);
        if (!enrollment.classId().equals(classId)) {
            throw new NoSuchElementException("Vínculo " + enrollmentId + " não encontrado nesta turma");
        }
        access.requireCanRemoveEnrollment(classId, enrollment);
        enrollmentRepo.delete(enrollmentId);
    }

    @Transactional
    public void selfUnenroll(Long classId) {
        access.requireVisibleClass(classId);
        Long personId = access.requireCurrentPersonId();
        BibleSchoolEnrollment enrollment = enrollmentRepo
                .find(BibleSchoolEnrollmentQuery.builder().classId(classId).personId(personId)
                        .role(BibleSchoolEnrollmentRoleEnum.STUDENT).build())
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Você não está matriculado nesta turma"));
        removeEnrollment(classId, enrollment.id());
    }

    @Transactional(readOnly = true)
    public List<BibleSchoolLesson> listLessons(Long classId) {
        access.requireVisibleClass(classId);
        if (access.canManageClass(classId)) {
            return lessonRepo.find(BibleSchoolLessonQuery.builder().classId(classId).build());
        }
        access.requireEnrolledStudent(classId);
        return lessonRepo.find(BibleSchoolLessonQuery.builder().classId(classId).status(BibleSchoolLessonStatusEnum.PUBLISHED).build());
    }

    @Transactional
    public BibleSchoolLesson createLesson(Long classId, BibleSchoolLessonForm form) {
        access.requireVisibleClass(classId);
        access.requireStaffOrTeacher(classId);
        validateLessonForm(form);
        return lessonRepo.insert(new BibleSchoolLesson(
                null, classId, form.title(), form.description(), form.lessonDate(),
                BibleSchoolLessonStatusEnum.DRAFT, null, null, securityUtils.getCurrentUserId()));
    }

    @Transactional
    public BibleSchoolLesson updateLesson(Long lessonId, BibleSchoolLessonForm form) {
        BibleSchoolLesson existing = requireLesson(lessonId);
        access.requireVisibleClass(existing.classId());
        access.requireStaffOrTeacher(existing.classId());
        validateLessonForm(form);
        BibleSchoolLessonStatusEnum status = form.status() != null ? form.status() : existing.status();
        return lessonRepo.update(new BibleSchoolLesson(
                lessonId, existing.classId(), form.title(), form.description(), form.lessonDate(),
                status, null, null, securityUtils.getCurrentUserId()));
    }

    @Transactional
    public void deleteLesson(Long lessonId) {
        BibleSchoolLesson lesson = requireLesson(lessonId);
        access.requireVisibleClass(lesson.classId());
        access.requireStaffOrTeacher(lesson.classId());
        lessonRepo.delete(lessonId);
    }

    @Transactional(readOnly = true)
    public List<BibleSchoolMaterial> listClassMaterials(Long classId) {
        access.requireVisibleClass(classId);
        if (!access.canManageClass(classId)) {
            access.requireEnrolledStudent(classId);
        }
        return materialRepo.find(BibleSchoolMaterialQuery.builder().classId(classId).generalOnly(true).build());
    }

    @Transactional(readOnly = true)
    public List<BibleSchoolMaterial> listLessonMaterials(Long lessonId) {
        BibleSchoolLesson lesson = requireLesson(lessonId);
        access.requireVisibleClass(lesson.classId());
        if (!access.canManageClass(lesson.classId())) {
            access.requireEnrolledStudent(lesson.classId());
            access.requirePublished(lesson);
        }
        return materialRepo.find(BibleSchoolMaterialQuery.builder().lessonId(lessonId).build());
    }

    @Transactional
    public BibleSchoolMaterial addClassMaterial(Long classId, MultipartFile file) {
        access.requireVisibleClass(classId);
        access.requireStaffOrTeacher(classId);
        return insertMaterial(classId, null, file);
    }

    @Transactional
    public BibleSchoolMaterial addLessonMaterial(Long lessonId, MultipartFile file) {
        BibleSchoolLesson lesson = requireLesson(lessonId);
        access.requireVisibleClass(lesson.classId());
        access.requireStaffOrTeacher(lesson.classId());
        return insertMaterial(lesson.classId(), lessonId, file);
    }

    @Transactional
    public void deleteMaterial(Long materialId) {
        BibleSchoolMaterial material = requireMaterial(materialId);
        access.requireVisibleClass(material.classId());
        access.requireStaffOrTeacher(material.classId());
        materialRepo.delete(materialId);
    }

    @Transactional(readOnly = true)
    public BibleSchoolMaterial downloadMaterial(Long materialId) {
        BibleSchoolMaterial material = requireMaterial(materialId);
        access.requireVisibleClass(material.classId());
        if (!access.canManageClass(material.classId())) {
            access.requireEnrolledStudent(material.classId());
            if (material.lessonId() != null) {
                access.requirePublished(requireLesson(material.lessonId()));
            }
        }
        return materialRepo.findWithData(materialId);
    }

    private BibleSchoolMaterial insertMaterial(Long classId, Long lessonId, MultipartFile file) {
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
        return materialRepo.insert(new BibleSchoolMaterial(
                null, classId, lessonId, fileName, file.getContentType(), file.getSize(),
                data, null, securityUtils.getCurrentUserId()));
    }

    // personId no body = STAFF ou professor da turma; ausente = o próprio chamador. @PreAuthorize não enxerga o body.
    @Transactional
    public BibleSchoolAttendance markAttendance(Long lessonId, BibleSchoolAttendanceForm form) {
        BibleSchoolLesson lesson = requireLesson(lessonId);
        access.requireVisibleClass(lesson.classId());
        access.requirePublished(lesson);
        Long personId;
        if (form != null && form.personId() != null) {
            access.requireStaffOrTeacher(lesson.classId());
            personId = form.personId();
        } else {
            personId = access.requireCurrentPersonId();
        }
        BibleSchoolEnrollment enrollment = enrollmentRepo
                .find(BibleSchoolEnrollmentQuery.builder().classId(lesson.classId()).personId(personId)
                        .role(BibleSchoolEnrollmentRoleEnum.STUDENT).build())
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Esta pessoa não está matriculada nesta turma"));
        if (!attendanceRepo.find(BibleSchoolAttendanceQuery.builder()
                .lessonId(lessonId).enrollmentId(enrollment.id()).build()).isEmpty()) {
            throw new IllegalArgumentException("Presença já registrada nesta aula");
        }
        boolean present = form == null || form.present() == null || form.present();
        return attendanceRepo.insert(new BibleSchoolAttendance(
                null, lessonId, enrollment.id(), null, null, present,
                null, null, securityUtils.getCurrentUserId()));
    }

    @Transactional
    public BibleSchoolAttendance updateAttendance(Long attendanceId, BibleSchoolAttendanceForm form) {
        BibleSchoolAttendance existing = requireAttendance(attendanceId);
        Long classId = requireLesson(existing.lessonId()).classId();
        access.requireVisibleClass(classId);
        access.requireCanMutateAttendance(classId, existing.personId());
        if (form == null || form.present() == null) {
            throw new IllegalArgumentException("Campo obrigatório: present");
        }
        return attendanceRepo.update(new BibleSchoolAttendance(
                attendanceId, existing.lessonId(), existing.enrollmentId(), null, null, form.present(),
                null, null, securityUtils.getCurrentUserId()));
    }

    @Transactional
    public void deleteAttendance(Long attendanceId) {
        BibleSchoolAttendance existing = requireAttendance(attendanceId);
        Long classId = requireLesson(existing.lessonId()).classId();
        access.requireVisibleClass(classId);
        access.requireCanMutateAttendance(classId, existing.personId());
        attendanceRepo.delete(attendanceId);
    }

    @Transactional(readOnly = true)
    public BibleSchoolAttendance getSelfAttendance(Long lessonId) {
        BibleSchoolLesson lesson = requireLesson(lessonId);
        access.requireVisibleClass(lesson.classId());
        Long personId = access.currentPersonId();
        if (personId == null) {
            throw new NoSuchElementException("Presença não encontrada");
        }
        BibleSchoolEnrollment enrollment = enrollmentRepo
                .find(BibleSchoolEnrollmentQuery.builder().classId(lesson.classId()).personId(personId)
                        .role(BibleSchoolEnrollmentRoleEnum.STUDENT).build())
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Presença não encontrada"));
        return attendanceRepo
                .find(BibleSchoolAttendanceQuery.builder().lessonId(lessonId).enrollmentId(enrollment.id()).build())
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Presença não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<BibleSchoolAttendance> listAttendance(Long lessonId) {
        BibleSchoolLesson lesson = requireLesson(lessonId);
        access.requireVisibleClass(lesson.classId());
        access.requireStaffOrTeacher(lesson.classId());
        return attendanceRepo.find(BibleSchoolAttendanceQuery.builder().lessonId(lessonId).build());
    }

    private BibleSchoolCycle requireCycle(Long id) {
        return cycleRepo.find(BibleSchoolCycleQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Ciclo " + id + " não encontrado"));
    }

    private BibleSchoolEnrollment requireEnrollment(Long id) {
        return enrollmentRepo.find(BibleSchoolEnrollmentQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Vínculo " + id + " não encontrado"));
    }

    private BibleSchoolLesson requireLesson(Long id) {
        return lessonRepo.find(BibleSchoolLessonQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Aula " + id + " não encontrada"));
    }

    private BibleSchoolMaterial requireMaterial(Long id) {
        return materialRepo.find(BibleSchoolMaterialQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Material " + id + " não encontrado"));
    }

    private BibleSchoolAttendance requireAttendance(Long id) {
        return attendanceRepo.find(BibleSchoolAttendanceQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Presença " + id + " não encontrada"));
    }

    private Long requireCycleId(Long cycleId) {
        if (cycleId == null) {
            throw new IllegalArgumentException("Campo obrigatório: cycleId (toda turma pertence a um ciclo)");
        }
        return cycleId;
    }

    private void validateLessonForm(BibleSchoolLessonForm form) {
        if (form == null || form.title() == null || form.title().isBlank()) {
            throw new IllegalArgumentException("Título da aula é obrigatório");
        }
        if (form.lessonDate() == null) {
            throw new IllegalArgumentException("Data da aula é obrigatória");
        }
    }

    private void validateCycleName(BibleSchoolCycleForm form) {
        if (form == null || form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("Nome do ciclo é obrigatório");
        }
    }

    private void assertUniqueCycleName(String name, Long excludeId) {
        if (!cycleRepo.find(BibleSchoolCycleQuery.builder().name(name).excludeId(excludeId).build()).isEmpty()) {
            throw new IllegalArgumentException("Já existe um ciclo com o nome \"" + name + "\"");
        }
    }

    private void validateClassName(BibleSchoolClassForm form) {
        if (form == null || form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("Nome da turma é obrigatório");
        }
    }

    private void assertSingleActiveCycle(boolean active, Long excludeId) {
        if (active && !cycleRepo.find(BibleSchoolCycleQuery.builder().active(true).excludeId(excludeId).build()).isEmpty()) {
            throw new IllegalArgumentException("Já existe um ciclo ativo; desative-o antes de ativar outro");
        }
    }
}
