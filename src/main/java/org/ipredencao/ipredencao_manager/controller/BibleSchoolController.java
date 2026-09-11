package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.config.Roles;
import org.ipredencao.ipredencao_manager.controller.form.BibleSchoolAttendanceForm;
import org.ipredencao.ipredencao_manager.controller.form.BibleSchoolClassForm;
import org.ipredencao.ipredencao_manager.controller.form.BibleSchoolCycleForm;
import org.ipredencao.ipredencao_manager.controller.form.BibleSchoolEnrollmentForm;
import org.ipredencao.ipredencao_manager.controller.form.BibleSchoolLessonForm;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolAttendance;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClass;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolClassQuery;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolCycle;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolEnrollment;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolLesson;
import org.ipredencao.ipredencao_manager.model.bible_school.BibleSchoolMaterial;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.service.BibleSchoolService;
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

// Matcher .authenticated() em /api/bible-school/**: MEMBER/MEMBERSHIP_CANDIDATE
// ficam de fora dos Roles; hasAnyRole(...) os barraria antes do @PreAuthorize.
@RestController
@RequestMapping("/api/bible-school")
public class BibleSchoolController {

    private static final String STAFF = Roles.STAFF_EXPR;
    private static final String AUTHENTICATED = "isAuthenticated()";

    @Autowired
    private BibleSchoolService service;

    @GetMapping("/cycles")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<BibleSchoolCycle>> listCycles() {
        return ResponseEntity.ok(service.listCycles());
    }

    @PostMapping("/cycles")
    @PreAuthorize(STAFF)
    public ResponseEntity<BibleSchoolCycle> createCycle(@RequestBody BibleSchoolCycleForm form) {
        return ResponseEntity.ok(service.createCycle(form));
    }

    @PutMapping("/cycles/{id}")
    @PreAuthorize(STAFF)
    public ResponseEntity<BibleSchoolCycle> updateCycle(@PathVariable Long id, @RequestBody BibleSchoolCycleForm form) {
        return ResponseEntity.ok(service.updateCycle(id, form));
    }

    @PostMapping("/classes/search")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<PagedResponse<BibleSchoolClass>> searchClasses(@RequestBody BibleSchoolClassQuery query) {
        return ResponseEntity.ok(service.searchClasses(query));
    }

    @GetMapping("/classes/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<BibleSchoolClass> getClass(@PathVariable Long id) {
        return ResponseEntity.ok(service.getClassDetail(id));
    }

    @PostMapping("/classes")
    @PreAuthorize(STAFF)
    public ResponseEntity<BibleSchoolClass> createClass(@RequestBody BibleSchoolClassForm form) {
        return ResponseEntity.ok(service.createClass(form));
    }

    @PutMapping("/classes/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<BibleSchoolClass> updateClass(@PathVariable Long id, @RequestBody BibleSchoolClassForm form) {
        return ResponseEntity.ok(service.updateClass(id, form));
    }

    @GetMapping("/classes/{id}/enrollments")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<BibleSchoolEnrollment>> listEnrollments(@PathVariable Long id) {
        return ResponseEntity.ok(service.listEnrollments(id));
    }

    @PostMapping("/classes/{id}/enrollments")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<BibleSchoolEnrollment> addEnrollment(@PathVariable Long id, @RequestBody(required = false) BibleSchoolEnrollmentForm form) {
        return ResponseEntity.ok(service.addEnrollment(id, form));
    }

    @DeleteMapping("/classes/{id}/enrollments/{enrollmentId}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Void> removeEnrollment(@PathVariable Long id, @PathVariable Long enrollmentId) {
        service.removeEnrollment(id, enrollmentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/classes/{id}/enrollments/me")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Void> selfUnenroll(@PathVariable Long id) {
        service.selfUnenroll(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/classes/{id}/lessons")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<BibleSchoolLesson>> listLessons(@PathVariable Long id) {
        return ResponseEntity.ok(service.listLessons(id));
    }

    @PostMapping("/classes/{id}/lessons")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<BibleSchoolLesson> createLesson(@PathVariable Long id, @RequestBody BibleSchoolLessonForm form) {
        return ResponseEntity.ok(service.createLesson(id, form));
    }

    @PutMapping("/lessons/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<BibleSchoolLesson> updateLesson(@PathVariable Long id, @RequestBody BibleSchoolLessonForm form) {
        return ResponseEntity.ok(service.updateLesson(id, form));
    }

    @DeleteMapping("/lessons/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Void> deleteLesson(@PathVariable Long id) {
        service.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/classes/{id}/materials")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<BibleSchoolMaterial>> listClassMaterials(@PathVariable Long id) {
        return ResponseEntity.ok(service.listClassMaterials(id));
    }

    @PostMapping("/classes/{id}/materials")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<BibleSchoolMaterial> addClassMaterial(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.addClassMaterial(id, file));
    }

    @GetMapping("/lessons/{id}/materials")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<BibleSchoolMaterial>> listLessonMaterials(@PathVariable Long id) {
        return ResponseEntity.ok(service.listLessonMaterials(id));
    }

    @PostMapping("/lessons/{id}/materials")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<BibleSchoolMaterial> addLessonMaterial(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
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
        BibleSchoolMaterial material = service.downloadMaterial(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(material.fileName()).build());
        MediaType mediaType = material.contentType() != null
                ? MediaType.parseMediaType(material.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().headers(headers).contentType(mediaType).body(material.data());
    }

    @PostMapping("/lessons/{id}/attendance")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<BibleSchoolAttendance> markAttendance(@PathVariable Long id, @RequestBody(required = false) BibleSchoolAttendanceForm form) {
        return ResponseEntity.ok(service.markAttendance(id, form));
    }

    @GetMapping("/lessons/{id}/attendance/me")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<BibleSchoolAttendance> getSelfAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSelfAttendance(id));
    }

    @GetMapping("/lessons/{id}/attendance")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<BibleSchoolAttendance>> listAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(service.listAttendance(id));
    }

    @PatchMapping("/attendance/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<BibleSchoolAttendance> updateAttendance(@PathVariable Long id, @RequestBody BibleSchoolAttendanceForm form) {
        return ResponseEntity.ok(service.updateAttendance(id, form));
    }

    @DeleteMapping("/attendance/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id) {
        service.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }
}
