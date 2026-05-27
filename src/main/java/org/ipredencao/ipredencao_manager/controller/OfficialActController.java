package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActCreateDto;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActQuery;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActType;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActUpdateDto;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.MinuteReportResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.repository.OfficialActCatalogRepository;
import org.ipredencao.ipredencao_manager.service.MinuteReportService;
import org.ipredencao.ipredencao_manager.service.OfficialActService;
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
@RequestMapping("/api/atos-oficiais")
@PreAuthorize("hasAnyRole('PRESBITERO', 'ADMIN')")
public class OfficialActController {

    private final OfficialActService officialActService;
    private final MinuteReportService minuteReportService;
    private final OfficialActCatalogRepository catalog;

    public OfficialActController(OfficialActService officialActService,
                                 MinuteReportService minuteReportService,
                                 OfficialActCatalogRepository catalog) {
        this.officialActService = officialActService;
        this.minuteReportService = minuteReportService;
        this.catalog = catalog;
    }

    @PostMapping
    public ResponseEntity<List<OfficialAct>> create(@RequestBody OfficialActCreateDto dto) {
        // Defesa: as flags de backfill nunca devem ser definidas por callers HTTP.
        if (dto != null) {
            dto.setSkipEffects(false);
            dto.setSkipNumeroOrdemAdmissao(false);
        }
        return ResponseEntity.ok(officialActService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfficialAct> findById(@PathVariable Long id) {
        return ResponseEntity.ok(officialActService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfficialAct> update(@PathVariable Long id, @RequestBody OfficialActUpdateDto dto) {
        return ResponseEntity.ok(officialActService.update(id, dto));
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

    @GetMapping("/atas/{minuteNumber}")
    public ResponseEntity<List<OfficialAct>> findByMinuteNumber(@PathVariable String minuteNumber) {
        return ResponseEntity.ok(officialActService.findByMinuteNumber(minuteNumber));
    }

    @GetMapping("/atas/{minuteNumber}/relatorio")
    public ResponseEntity<MinuteReportResponse> minuteReport(@PathVariable String minuteNumber) {
        return ResponseEntity.ok(minuteReportService.generate(minuteNumber));
    }

    @GetMapping("/catalogo")
    public ResponseEntity<List<OfficialActType>> catalog() {
        return ResponseEntity.ok(catalog.getTypes());
    }
}
