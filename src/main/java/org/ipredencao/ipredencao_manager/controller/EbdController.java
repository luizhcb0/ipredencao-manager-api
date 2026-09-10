package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.config.Roles;
import org.ipredencao.ipredencao_manager.controller.form.EbdClassForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdCycleForm;
import org.ipredencao.ipredencao_manager.controller.form.EbdEnrollmentForm;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClass;
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdCycle;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollment;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.service.EbdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
