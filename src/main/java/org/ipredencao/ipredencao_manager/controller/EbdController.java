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
import org.ipredencao.ipredencao_manager.model.ebd.EbdClassSyllabus;
import org.ipredencao.ipredencao_manager.model.ebd.EbdCycle;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollment;
import org.ipredencao.ipredencao_manager.model.ebd.EbdLesson;
import org.ipredencao.ipredencao_manager.model.ebd.EbdMaterial;
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

// SecurityConfig só exige "autenticado" pra todo /api/ebd/** (ver matcher lá):
// MEMBER/MEMBERSHIP_CANDIDATE (V014) ficam de fora de todos os grupos de
// Roles, mas precisam alcançar os endpoints de automatrícula/presença — um
// matcher hasAnyRole(...) os barraria antes de chegar aqui. Toda autorização
// real vive em @PreAuthorize por método: STAFF nos endpoints puramente
// administrativos, isAuthenticated() nos que servem os dois papéis (matrícula,
// presença) ou só leitura/self-service, com a distinção fina resolvida no
// service quando o @PreAuthorize sozinho não dá conta.
@RestController
@RequestMapping("/api/ebd")
public class EbdController {

    private static final String STAFF = Roles.STAFF_EXPR;
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
    // Toda turma pertence a um ciclo (cycleId obrigatório) — sem distinção
    // "turma fixa" (removida a pedido do time em revisão do PR).

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
    @PreAuthorize(STAFF)
    public ResponseEntity<EbdClass> updateClass(@PathVariable Long id, @RequestBody EbdClassForm form) {
        return ResponseEntity.ok(service.updateClass(id, form));
    }

    // Ementa: arquivo (BYTEA — sem S3), no máximo um por turma.
    @PostMapping("/classes/{id}/syllabus")
    @PreAuthorize(STAFF)
    public ResponseEntity<EbdClass> uploadSyllabus(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(service.uploadSyllabus(id, file));
    }

    @GetMapping("/classes/{id}/syllabus/download")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<byte[]> downloadSyllabus(@PathVariable Long id) {
        EbdClassSyllabus syllabus = service.downloadSyllabus(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(syllabus.fileName()).build());
        MediaType mediaType = syllabus.contentType() != null
                ? MediaType.parseMediaType(syllabus.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().headers(headers).contentType(mediaType).body(syllabus.data());
    }

    @DeleteMapping("/classes/{id}/syllabus")
    @PreAuthorize(STAFF)
    public ResponseEntity<EbdClass> deleteSyllabus(@PathVariable Long id) {
        return ResponseEntity.ok(service.deleteSyllabus(id));
    }

    // ===== Matrícula =====
    // POST/DELETE .../enrollments[/{id}] servem os dois casos: personId no
    // body presente = matrícula administrativa (STAFF, qualquer role); ausente
    // = automatrícula (qualquer autenticado com pessoa vinculada, sempre
    // STUDENT) — decidido no service, que também resolve "é o próprio aluno
    // removendo a própria matrícula" no DELETE. .../enrollments/me continua só
    // pro DELETE, como atalho pra quem não sabe o próprio enrollmentId.

    @GetMapping("/classes/{id}/enrollments")
    @PreAuthorize(STAFF)
    public ResponseEntity<List<EbdEnrollment>> listEnrollments(@PathVariable Long id) {
        return ResponseEntity.ok(service.listEnrollments(id));
    }

    @PostMapping("/classes/{id}/enrollments")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<EbdEnrollment> addEnrollment(@PathVariable Long id, @RequestBody(required = false) EbdEnrollmentForm form) {
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

    // ===== Aula =====
    // updateLesson/deleteLesson recebem o id da AULA, não da turma; a checagem
    // (STAFF) fica no service (EbdService.requireStaff) pra ficar no mesmo
    // lugar dos outros métodos administrativos de aula/material que têm o
    // mesmo formato de path variable.

    @GetMapping("/classes/{id}/lessons")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<EbdLesson>> listLessons(@PathVariable Long id) {
        return ResponseEntity.ok(service.listLessons(id));
    }

    @PostMapping("/classes/{id}/lessons")
    @PreAuthorize(STAFF)
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
    @PreAuthorize(STAFF)
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
        EbdMaterial material = service.downloadMaterial(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(material.fileName()).build());
        MediaType mediaType = material.contentType() != null
                ? MediaType.parseMediaType(material.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().headers(headers).contentType(mediaType).body(material.data());
    }

    // ===== Presença =====
    // POST .../attendance cria, PATCH/DELETE .../attendance/{id} atualiza ou
    // remove — servem professor e aluno: personId no body (só STAFF pode
    // informar) marca/altera em nome de outra pessoa; sem personId, é sempre em
    // nome de quem chama. listAttendance/updateAttendance/deleteAttendance
    // recebem id de aula/presença, não de turma — mesma razão de aula/material:
    // a parte que é STAFF-only fica no service.

    @PostMapping("/lessons/{id}/attendance")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<EbdAttendance> markAttendance(@PathVariable Long id, @RequestBody(required = false) EbdAttendanceForm form) {
        return ResponseEntity.ok(service.markAttendance(id, form));
    }

    // 404 quando não há registro — cobre "nunca marcou" e "não está
    // matriculado" da mesma forma; o front só precisa saber se mostra ou não
    // o botão de marcar presença.
    @GetMapping("/lessons/{id}/attendance/me")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<EbdAttendance> getSelfAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSelfAttendance(id));
    }

    @GetMapping("/lessons/{id}/attendance")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<List<EbdAttendance>> listAttendance(@PathVariable Long id) {
        return ResponseEntity.ok(service.listAttendance(id));
    }

    @PatchMapping("/attendance/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<EbdAttendance> updateAttendance(@PathVariable Long id, @RequestBody EbdAttendanceForm form) {
        return ResponseEntity.ok(service.updateAttendance(id, form));
    }

    @DeleteMapping("/attendance/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id) {
        service.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }
}
