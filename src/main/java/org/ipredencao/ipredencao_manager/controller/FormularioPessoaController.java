package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.*;
import org.ipredencao.ipredencao_manager.service.FormularioPessoaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/formulario-pessoa")
public class FormularioPessoaController {
    @Autowired
    private FormularioPessoaService service;

    @PostMapping
    public ResponseEntity<FormularioPessoa> criar(@RequestBody FormularioPessoa formulario) {
        FormularioPessoa criado = service.criar(formulario);
        return ResponseEntity.ok(criado);
    }

    @PostMapping("/{id}/foto")
    public ResponseEntity<FormularioPessoa> uploadFoto(
        @PathVariable Long id,
        @RequestParam("foto") MultipartFile foto
    ) throws IOException {
        FormularioPessoa formulario = service.findById(id);
        if (formulario == null) {
            return ResponseEntity.notFound().build();
        }
        FormularioPessoa updated = service.savePhoto(formulario, foto);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormularioPessoa> atualizar(@PathVariable Long id, @RequestBody FormularioPessoa formulario) {
        formulario.setId(id);
        FormularioPessoa atualizado = service.atualizar(formulario);
        return ResponseEntity.ok(atualizado);
    }

    @PostMapping("/search")
    public ResponseEntity<List<FormularioPessoa>> buscarFormularioPessoas(@RequestBody FormularioPessoaQuery query) {
        List<FormularioPessoa> formularios = service.find(query);
        return ResponseEntity.ok(formularios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormularioPessoa> buscarPorId(@PathVariable Long id) {
        FormularioPessoa encontrado = service.findById(id);
        if (encontrado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(encontrado);
    }

    @PostMapping("/processar")
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