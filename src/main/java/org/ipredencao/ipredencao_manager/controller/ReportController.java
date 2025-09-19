package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.*;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.service.ReportService;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    
    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private PessoaService pessoaService;
    
    /**
     * Endpoint para obter resumo de pessoas e formulários por categoria
     * Acesso: BOLETIM, PRESBITERO, ADMIN
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> getSummary() {
        try {
            log.info("Requisição para obter resumo de relatórios");
            SummaryResponse summary = reportService.generateSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Erro ao gerar resumo de relatórios: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }


    @PostMapping("/generate-report")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> generateReport(@RequestBody PessoaQuery query) {
        try {
            // Validação básica
            if (query == null) {
                query = PessoaQuery.builder().build(); // Query vazia retorna todas as pessoas
            }
            List<Pessoa> people = pessoaService.find(query);
            
            log.info("Retornando {} pessoas para geração de PDF", people.size());
            return ResponseEntity.ok(people);
            
        } catch (Exception e) {
            log.error("Erro ao obter pessoas para PDF: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
}
