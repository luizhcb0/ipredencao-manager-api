package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.*;
import org.ipredencao.ipredencao_manager.service.FormularioPessoaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/formulario-pessoa")
public class FormularioPessoaController {
    
    @Autowired
    private FormularioPessoaService service;

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody FormularioPessoa formulario, HttpServletRequest request) {
        try {
            FormularioPessoa criado = service.criar(formulario);
            
            // Auditoria para usuário anônimo (sem autenticação)
            // TODO: salvar estes dados de quem criou.
//            request.getRemoteAddr(),
//            request.getHeader("User-Agent")
            return ResponseEntity.ok(criado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }

    @PostMapping("/{id}/foto")
    public ResponseEntity<?> uploadFoto(
        @PathVariable Long id,
        @RequestParam("foto") MultipartFile foto
    ) {
        try {
            FormularioPessoa formulario = service.findById(id);
            if (formulario == null) {
                return ResponseEntity.status(404).body(new ErrorResponse("Formulário não encontrado"));
            }
            FormularioPessoa updated = service.savePhoto(formulario, foto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro ao fazer upload", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody FormularioPessoa formulario) {
        try {
            formulario.setId(id);
            FormularioPessoa atualizado = service.atualizar(formulario);
            return ResponseEntity.ok(atualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> buscarFormularioPessoas(@RequestBody FormularioPessoaQuery query) {
        try {
            List<FormularioPessoa> formularios = service.find(query);
            return ResponseEntity.ok(formularios);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro na busca", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            FormularioPessoa encontrado = service.findById(id);
            return ResponseEntity.ok(encontrado);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Formulário não encontrado"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }

    @PostMapping("/processar")
    @PreAuthorize("hasAnyRole('PRESBITERO', 'ADMIN')")
    public ResponseEntity<ProcessarFormularioResponse> processarFormulario(
            @RequestBody ProcessarFormularioRequest request) {
        try {
            ProcessarFormularioResponse response = service.processarFormulario(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            ProcessarFormularioResponse errorResponse = new ProcessarFormularioResponse();
            errorResponse.setMensagem("Erro: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            ProcessarFormularioResponse errorResponse = new ProcessarFormularioResponse();
            errorResponse.setMensagem("Erro interno: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
} 