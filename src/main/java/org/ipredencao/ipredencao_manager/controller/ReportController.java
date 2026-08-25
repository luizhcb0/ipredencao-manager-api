package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.config.Roles;
import org.ipredencao.ipredencao_manager.model.SummaryResponse;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.service.ReportService;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private PessoaService pessoaService;

    @GetMapping("/summary")
    @PreAuthorize(Roles.ANY_ROLE_EXPR)
    public ResponseEntity<SummaryResponse> getSummary() {
        return ResponseEntity.ok(reportService.generateSummary());
    }

    @PostMapping("/generate-report")
    @PreAuthorize(Roles.ANY_ROLE_EXPR)
    public ResponseEntity<List<Pessoa>> generateReport(@RequestBody PessoaQuery query) {
        if (query == null) {
            query = PessoaQuery.builder().build();
        }
        return ResponseEntity.ok(pessoaService.find(query));
    }
}
