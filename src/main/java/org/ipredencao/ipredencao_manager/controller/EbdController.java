package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.config.Roles;
import org.ipredencao.ipredencao_manager.controller.form.EbdAttendanceForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdClassForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdCycleForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdEnrollmentForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdLessonForm;
import org.ipredencao.ipredencao_manager.model.ebd.EbdAttendance;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClass;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdCycle;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollment;
import org.ipredencao.ipredencao_manager.model.ebd.EbdLesson;
import org.ipredencao.ipredencao_manager.model.ebd.EbdMaterial;
import org.ipredencao.ipredencao_manager.model.ebd.EbdMaterialContent;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.service.EbdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Sem matcher próprio em SecurityConfig (deliberado): MEMBER/MEMBERSHIP_CANDIDATE
// (V014) ficam de fora de todos os grupos de Roles, mas precisam alcançar os
// endpoints de automatrícula/presença — então /api/ebd/** cai no catch-all
// `anyRequest().authenticated()` (só exige um JWT válido) e toda autorização
// real vive aqui, em @PreAuthorize por método (STAFF, professor-da-turma via
// EbdAccess, ou qualquer autenticado nos endpoints "/me").
@RestController
@RequestMapping("/api/ebd")
public class EbdController {

    private static final String STAFF = Roles.STAFF_EXPR;
    private static final String STAFF_OR_TEACHER = "@roles.staff(authentication) or @ebdAccess.isTeacherOf(authentication, #id)";
    private static final String AUTHENTICATED = "isAuthenticated()";

    @Autowired
    private EbdService service;

    // ===== Ciclo =====

    @GetMapping("/cycles")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<EbdCycle>> listCycles() {
        return ResponseEntity.ok(service.listCycles());
    }

    @PostMapping("/cycles")
    @PreAuthorize(STAFF)
    public ResponseEntity<EbdCycle> createCycle(@RequestBody EbdCycleForm form) {
        return ResponseEntity.ok(service.createCycle(form));
    }

    @PutMapping("/cycles/{id}")
    @PreAuthorize(STAFF)
    public ResponseEntity<EbdCycle> updateCycle(@PathVariable Long id, @RequestBody EbdCycleForm form) {
        return ResponseEntity.ok(service.updateCycle(id, form));
    }

    // ===== Turma =====

    @PostMapping("/classes/search")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<PagedResponse<EbdClass>> searchClasses(@RequestBody EbdClassQuery query) {
        return ResponseEntity.ok(service.searchClasses(query));
    }

    @GetMapping("/classes/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<EbdClass> getClass(@PathVariable Long id) {
        return ResponseEntity.ok(service.getClassDetail(id));
    }

    @PostMapping("/classes")
    @PreAuthorize(STAFF)
    public ResponseEntity<EbdClass> createClass(@RequestBody EbdClassForm form) {
        return ResponseEntity.ok(service.createClass(form));
    }

    @PutMapping("/classes/{id}")
    @PreAuthorize(STAFF_OR_TEACHER)
    public ResponseEntity<EbdClass> updateClass(@PathVariable Long id, @RequestBody EbdClassForm form) {
        return ResponseEntity.ok(service.updateClass(id, form));
    }

    // ===== Matrícula administrativa (STAFF ou professor-da-turma) =====
    // A checagem fina — professor só inclui STUDENT, só STAFF associa/remove
    // TEACHER, STUDENT só em turma fixa — fica no service (não dá para expressar
    // "olhe o role do body" em @PreAuthorize).

    @GetMapping("/classes/{id}/enrollments")
    @PreAuthorize(STAFF_OR_TEACHER)
    public ResponseEntity<List<EbdEnrollment>> listEnrollments(@PathVariable Long id) {
        return ResponseEntity.ok(service.listEnrollments(id));
    }

    @PostMapping("/classes/{id}/enrollments")
    @PreAuthorize(STAFF_OR_TEACHER)
    public ResponseEntity<EbdEnrollment> addEnrollment(@PathVariable Long id, @RequestBody EbdEnrollmentForm form) {
        return ResponseEntity.ok(service.addEnrollment(id, form));
    }

    @DeleteMapping("/classes/{id}/enrollments/{enrollmentId}")
    @PreAuthorize(STAFF_OR_TEACHER)
    public ResponseEntity<Void> removeEnrollment(@PathVariable Long id, @PathVariable Long enrollmentId) {
        service.removeEnrollment(id, enrollmentId);
        return ResponseEntity.noContent().build();
    }

    // ===== Automatrícula (turma não-fixa) =====

    @PostMapping("/classes/{id}/enrollments/me")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<EbdEnrollment> selfEnroll(@PathVariable Long id) {
        return ResponseEntity.ok(service.selfEnroll(id));
    }

    @DeleteMapping("/classes/{id}/enrollments/me")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Void> selfUnenroll(@PathVariable Long id) {
        service.selfUnenroll(id);
        return ResponseEntity.noContent().build();
    }

    // ===== Aula =====
    // updateLesson/deleteLesson recebem o id da AULA, não da turma — não dá
    // para expressar "STAFF ou professor da turma" em @PreAuthorize com esse
    // path variable, então a checagem fica no service (EbdService.requireStaffOrTeacher).

    @GetMapping("/classes/{id}/lessons")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<EbdLesson>> listLessons(@PathVariable Long id) {
        return ResponseEntity.ok(service.listLessons(id));
    }

    @PostMapping("/classes/{id}/lessons")
    @PreAuthorize(STAFF_OR_TEACHER)
    public ResponseEntity<EbdLesson> createLesson(@PathVariable Long id, @RequestBody EbdLessonForm form) {
        return ResponseEntity.ok(service.createLesson(id, form));
    }

    @PutMapping("/lessons/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<EbdLesson> updateLesson(@PathVariable Long id, @RequestBody EbdLessonForm form) {
        return ResponseEntity.ok(service.updateLesson(id, form));
    }

    @DeleteMapping("/lessons/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        service.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    // ===== Material =====
    // Guardado como BYTEA na própria tabela (decisão do usuário — sem S3).

    @GetMapping("/classes/{id}/materials")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<EbdMaterial>> listClassMaterials(@PathVariable Long id) {
        return ResponseEntity.ok(service.listClassMaterials(id));
    }

    @PostMapping("/classes/{id}/materials")
    @PreAuthorize(STAFF_OR_TEACHER)
    public ResponseEntity<EbdMaterial> addClassMaterial(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.addClassMaterial(id, file));
    }

    @GetMapping("/lessons/{id}/materials")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<EbdMaterial>> listLessonMaterials(@PathVariable Long id) {
        return ResponseEntity.ok(service.listLessonMaterials(id));
    }

    @PostMapping("/lessons/{id}/materials")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<EbdMaterial> addLessonMaterial(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.addLessonMaterial(id, file));
    }

    @DeleteMapping("/materials/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Void> deleteMaterial(@PathVariable Long id) {
        service.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/materials/{id}/download")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<byte[]> downloadMaterial(@PathVariable Long id) {
        EbdMaterialContent content = service.downloadMaterial(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(content.fileName()).build());
        MediaType mediaType = content.contentType() != null
                ? MediaType.parseMediaType(content.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().headers(headers).contentType(mediaType).body(content.data());
    }

    // ===== Presença =====
    // listAttendance/rectifyAttendance recebem id de aula/presença, não de
    // turma — mesma razão de aula/material: checagem fica no service.

    @PostMapping("/lessons/{id}/attendance/me")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<EbdAttendance> selfReportAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(service.selfReportAttendance(id));
    }

    @GetMapping("/lessons/{id}/attendance")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<EbdAttendance>> listAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(service.listAttendance(id));
    }

    @PatchMapping("/attendance/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<EbdAttendance> rectifyAttendance(@PathVariable Long id, @RequestBody EbdAttendanceForm form) {
        return ResponseEntity.ok(service.rectifyAttendance(id, form));
    }
}
