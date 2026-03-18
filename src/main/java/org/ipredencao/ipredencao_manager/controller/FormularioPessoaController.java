package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoaQuery;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.ProcessarFormularioRequest;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.ProcessarFormularioResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.service.FormularioPessoaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/formulario-pessoa")
public class FormularioPessoaController {

    @Autowired
    private FormularioPessoaService service;

    @PostMapping
    public ResponseEntity<FormularioPessoa> create(@RequestBody FormularioPessoa formulario) {
        return ResponseEntity.ok(service.create(formulario));
    }

    @PostMapping("/{id}/foto")
    public ResponseEntity<FormularioPessoa> uploadPhoto(@PathVariable Long id, @RequestParam("foto") MultipartFile foto) throws IOException {
        FormularioPessoa formulario = service.findById(id);
        if (formulario == null) {
            throw new NoSuchElementException("Form not found with id: " + id);
        }
        return ResponseEntity.ok(service.savePhoto(formulario, foto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormularioPessoa> update(@PathVariable Long id, @RequestBody FormularioPessoa formulario) {
        formulario.setId(id);
        return ResponseEntity.ok(service.update(formulario));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('BOLETIM', 'DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<PagedResponse<FormularioPessoa>> searchForms(@RequestBody FormularioPessoaQuery query) {
        return ResponseEntity.ok(service.findPaginated(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BOLETIM', 'DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<FormularioPessoa> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping("/processar")
    @PreAuthorize("hasAnyRole('DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<ProcessarFormularioResponse> processForm(@RequestBody ProcessarFormularioRequest request) {
        return ResponseEntity.ok(service.processForm(request));
    }
}
