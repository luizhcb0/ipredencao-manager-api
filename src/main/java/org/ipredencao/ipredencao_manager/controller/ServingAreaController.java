package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.controller.form.ServingAreaForm;
import org.ipredencao.ipredencao_manager.controller.form.ServingAreaMemberForm;
import org.ipredencao.ipredencao_manager.controller.form.ServingAreaPositionForm;
import org.ipredencao.ipredencao_manager.controller.form.ServingAreaTeamForm;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.serving_area.ParticipationReportQuery;
import org.ipredencao.ipredencao_manager.model.serving_area.ParticipationReportRow;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingArea;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaMember;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaMemberQuery;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPosition;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaQuery;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaTeam;
import org.ipredencao.ipredencao_manager.service.ServingAreaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Leitura: BOLETIM+. Escrita: DIACONO+. Autorização em dupla camada (aqui e no
// SecurityConfig). @PreAuthorize por método (não na classe) porque search e o
// relatório são POST de leitura, liberados ao BOLETIM.
@RestController
@RequestMapping("/api/serving-areas")
public class ServingAreaController {

    private static final String READ = "hasAnyRole('BOLETIM','DIACONO','PRESBITERO','ADMIN')";
    private static final String WRITE = "hasAnyRole('DIACONO','PRESBITERO','ADMIN')";

    @Autowired
    private ServingAreaService service;

    // ===== Leitura =====

    @PostMapping("/search")
    @PreAuthorize(READ)
    public ResponseEntity<PagedResponse<ServingArea>> search(@RequestBody ServingAreaQuery query) {
        return ResponseEntity.ok(service.findPaginated(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize(READ)
    public ResponseEntity<ServingArea> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findDetail(id));
    }

    @PostMapping("/members/search")
    @PreAuthorize(READ)
    public ResponseEntity<List<ServingAreaMember>> searchMembers(@RequestBody ServingAreaMemberQuery query) {
        return ResponseEntity.ok(service.findMembers(query));
    }

    @PostMapping("/participation-report")
    @PreAuthorize(READ)
    public ResponseEntity<List<ParticipationReportRow>> participationReport(@RequestBody ParticipationReportQuery query) {
        return ResponseEntity.ok(service.participationReport(query));
    }

    // ===== Serviço (área) =====

    @PostMapping
    @PreAuthorize(WRITE)
    public ResponseEntity<ServingArea> create(@RequestBody ServingAreaForm form) {
        return ResponseEntity.ok(service.create(form));
    }

    @PutMapping("/{id}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ServingArea> update(@PathVariable Long id, @RequestBody ServingAreaForm form) {
        return ResponseEntity.ok(service.update(id, form));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(WRITE)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ===== Cargos =====

    @PostMapping("/{id}/positions")
    @PreAuthorize(WRITE)
    public ResponseEntity<ServingAreaPosition> addPosition(@PathVariable Long id, @RequestBody ServingAreaPositionForm form) {
        return ResponseEntity.ok(service.addPosition(id, form));
    }

    @PutMapping("/{id}/positions/{positionId}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ServingAreaPosition> updatePosition(@PathVariable Long id, @PathVariable Long positionId,
                                                              @RequestBody ServingAreaPositionForm form) {
        return ResponseEntity.ok(service.updatePosition(id, positionId, form));
    }

    @DeleteMapping("/{id}/positions/{positionId}")
    @PreAuthorize(WRITE)
    public ResponseEntity<Void> deletePosition(@PathVariable Long id, @PathVariable Long positionId) {
        service.deletePosition(id, positionId);
        return ResponseEntity.noContent().build();
    }

    // ===== Equipes =====

    @PostMapping("/{id}/teams")
    @PreAuthorize(WRITE)
    public ResponseEntity<ServingAreaTeam> addTeam(@PathVariable Long id, @RequestBody ServingAreaTeamForm form) {
        return ResponseEntity.ok(service.addTeam(id, form));
    }

    @PutMapping("/{id}/teams/{teamId}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ServingAreaTeam> updateTeam(@PathVariable Long id, @PathVariable Long teamId,
                                                      @RequestBody ServingAreaTeamForm form) {
        return ResponseEntity.ok(service.updateTeam(id, teamId, form));
    }

    @DeleteMapping("/{id}/teams/{teamId}")
    @PreAuthorize(WRITE)
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id, @PathVariable Long teamId) {
        service.deleteTeam(id, teamId);
        return ResponseEntity.noContent().build();
    }

    // ===== Vínculos (membros) =====

    @PostMapping("/{id}/members")
    @PreAuthorize(WRITE)
    public ResponseEntity<ServingAreaMember> addMember(@PathVariable Long id, @RequestBody ServingAreaMemberForm form) {
        return ResponseEntity.ok(service.addMember(id, form));
    }

    @PutMapping("/{id}/members/{memberId}")
    @PreAuthorize(WRITE)
    public ResponseEntity<ServingAreaMember> updateMember(@PathVariable Long id, @PathVariable Long memberId,
                                                          @RequestBody ServingAreaMemberForm form) {
        return ResponseEntity.ok(service.updateMember(id, memberId, form));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @PreAuthorize(WRITE)
    public ResponseEntity<Void> deleteMember(@PathVariable Long id, @PathVariable Long memberId) {
        service.deleteMember(id, memberId);
        return ResponseEntity.noContent().build();
    }
}
