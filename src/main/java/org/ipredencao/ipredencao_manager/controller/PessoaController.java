package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.relacionamento_pessoa.RelacionamentoPessoaIds;
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
import org.springframework.http.HttpStatus;
import org.ipredencao.ipredencao_manager.model.ErrorResponse;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.model.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.SubcategoriaEnum;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import java.util.List;
import org.ipredencao.ipredencao_manager.model.relacionamento_pessoa.RelacionamentoPessoa;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Map;

@RestController
@RequestMapping("/api/pessoas") // Atualizado para seguir padrão /api/*
public class PessoaController {
    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {this.pessoaService = pessoaService;}

    @PostMapping
    @PreAuthorize("hasAnyRole('PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> criarPessoa(@RequestBody Pessoa pessoa) {
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
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
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
    public ResponseEntity<?> buscarPessoas(@RequestBody PessoaQuery query) {
        try {
            List<Pessoa> pessoas = pessoaService.find(query);
            return ResponseEntity.ok(pessoas);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro na busca", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> atualizarPessoa(@PathVariable Long id, @RequestBody Pessoa pessoa) {
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

    // Relacionamentos qualificados
    @PostMapping("/{id}/relacionamentos")
    @PreAuthorize("hasAnyRole('PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> criarRelacionamento(@PathVariable Long id, @RequestBody RelacionamentoPessoaIds relacionamento) {
        try {
            return ResponseEntity.ok(pessoaService.criarRelacionamento(id, relacionamento));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Pessoa não encontrada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }

    @GetMapping("/{id}/relacionamentos")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> listarRelacionamentos(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(pessoaService.listarRelacionamentosPorPessoa(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Pessoa não encontrada"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    /**
     * Lista todas as subcategorias disponíveis
     */
    @GetMapping("/subcategorias")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> getAllSubcategorias() {
        try {
            return ResponseEntity.ok(pessoaService.getAllSubcategorias());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
}
