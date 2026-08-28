package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.config.Roles;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.controller.form.OfficialActCreateForm;
import org.ipredencao.ipredencao_manager.controller.form.OfficialActUpdateForm;
import org.ipredencao.ipredencao_manager.controller.form.MinuteUpdateForm;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActQuery;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActType;
import org.ipredencao.ipredencao_manager.model.official_act.MinuteResponse;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.MinuteReportResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.repository.OfficialActTypesRepository;
import org.ipredencao.ipredencao_manager.service.MinuteReportService;
import org.ipredencao.ipredencao_manager.service.MinuteService;
import org.ipredencao.ipredencao_manager.service.OfficialActService;
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

@RestController
@RequestMapping("/api/official-acts")
@PreAuthorize(Roles.ELDER_EXPR)
public class OfficialActController {

    @Autowired
    private OfficialActService officialActService;
    @Autowired
    private MinuteReportService minuteReportService;
    @Autowired
    private MinuteService minuteService;
    @Autowired
    private OfficialActTypesRepository officialActTypesRepository;

    @PostMapping
    public ResponseEntity<List<OfficialAct>> create(@RequestBody OfficialActCreateForm form) {
        if (form != null) {
            form.setSkipEffects(false);
            form.setSkipAdmissionOrderNumber(false);
        }
        return ResponseEntity.ok(officialActService.create(form));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfficialAct> findById(@PathVariable Long id) {
        return ResponseEntity.ok(officialActService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfficialAct> update(@PathVariable Long id, @RequestBody OfficialActUpdateForm form) {
        return ResponseEntity.ok(officialActService.update(id, form));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        officialActService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public ResponseEntity<PagedResponse<OfficialAct>> search(@RequestBody OfficialActQuery query) {
        return ResponseEntity.ok(officialActService.findPaginated(query));
    }

    @GetMapping("/minutes/{minuteNumber}")
    public ResponseEntity<List<OfficialAct>> findByMinuteNumber(@PathVariable String minuteNumber) {
        return ResponseEntity.ok(officialActService.findByMinuteNumber(minuteNumber));
    }

    @GetMapping("/minutes/{minuteNumber}/report")
    public ResponseEntity<MinuteReportResponse> minuteReport(@PathVariable String minuteNumber) {
        return ResponseEntity.ok(minuteReportService.generate(minuteNumber));
    }

    @PutMapping("/minutes/{minuteNumber}")
    public ResponseEntity<MinuteResponse> updateMinute(
            @PathVariable String minuteNumber,
            @RequestBody MinuteUpdateForm form) {
        return ResponseEntity.ok(minuteService.updateDate(
                minuteNumber,
                form != null ? form.minuteDate() : null));
    }

    @GetMapping("/types")
    public ResponseEntity<List<OfficialActType>> types() {
        return ResponseEntity.ok(officialActTypesRepository.getTypes());
    }
}
