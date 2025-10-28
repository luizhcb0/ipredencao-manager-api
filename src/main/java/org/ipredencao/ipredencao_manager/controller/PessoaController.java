package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistory;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.ipredencao.ipredencao_manager.model.ErrorResponse;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistoryResponse;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import java.util.List;
import java.io.IOException;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/pessoas") // Atualizado para seguir padrão /api/*
public class PessoaController {
    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {this.pessoaService = pessoaService;}

    private static final Logger log = LoggerFactory.getLogger(PessoaController.class);

    @PostMapping
    @PreAuthorize("hasAnyRole('PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> createPerson(@RequestBody Pessoa pessoa) {
        try {
            return ResponseEntity.ok(pessoaService.create(pessoa));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }

    @PostMapping("/{id}/foto")
    public ResponseEntity<?> uploadPhoto(
        @PathVariable Long id,
        @RequestParam("foto") MultipartFile foto
    ) {
        try {
            Pessoa pessoa = pessoaService.findById(id);
            Pessoa updated = pessoaService.savePhoto(pessoa, foto);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Pessoa não encontrada"));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro ao fazer upload", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            Pessoa pessoa = pessoaService.findById(id);
            return ResponseEntity.ok(pessoa);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Pessoa não encontrada"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> searchPeople(@RequestBody PessoaQuery query) {
        try {
            PagedResponse<Pessoa> response = pessoaService.findPaginated(query);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro na busca", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> updatePerson(@PathVariable Long id, @RequestBody Pessoa pessoa) {
        try {
            pessoa.setId(id);
            return ResponseEntity.ok(pessoaService.update(pessoa));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Pessoa não encontrada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }

    /**
     * Busca o histórico de alterações de uma pessoa por ID
     */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> findHistoryById(@PathVariable Long id) {
        try {
            List<PessoaHistory> history = pessoaService.findHistoryById(id);
            PessoaHistoryResponse pessoaHistoryResponse = new PessoaHistoryResponse(id, history);
            return ResponseEntity.ok(pessoaHistoryResponse);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Pessoa não encontrada"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }

    /**
     * Cria um relacionamento entre duas pessoas
     */
    @PostMapping("/{id}/relacionamento")
    @PreAuthorize("hasAnyRole('PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> createRelationship(
            @PathVariable Long id,
            @RequestBody Relacionamento relacionamento) {
        try {
            Relacionamento created = pessoaService.createRelationship(id, relacionamento);
            return ResponseEntity.ok(created);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Pessoa não encontrada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
}
