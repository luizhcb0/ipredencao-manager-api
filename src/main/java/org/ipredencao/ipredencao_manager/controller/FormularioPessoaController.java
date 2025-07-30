package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.FormularioPessoa;
import org.ipredencao.ipredencao_manager.service.FormularioPessoaService;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/formulario-pessoa")
public class FormularioPessoaController {
    @Autowired
    private FormularioPessoaService service;
    @Autowired
    private PessoaService pessoaService;

    @PostMapping
    public ResponseEntity<FormularioPessoa> criar(@RequestBody FormularioPessoa formulario) {
        FormularioPessoa criado = service.criar(formulario);
        return ResponseEntity.ok(criado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormularioPessoa> atualizar(@PathVariable Long id, @RequestBody FormularioPessoa formulario) {
        formulario.setId(id);
        FormularioPessoa atualizado = service.atualizar(formulario);
        return ResponseEntity.ok(atualizado);
    }

    @GetMapping
    public ResponseEntity<List<FormularioPessoa>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormularioPessoa> buscarPorId(@PathVariable Long id) {
        FormularioPessoa encontrado = service.buscarPorId(id);
        if (encontrado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(encontrado);
    }

    @PostMapping("/gerar-pessoa")
    public ResponseEntity<Pessoa> gerarPessoa(@RequestBody FormularioPessoa formulario) {
        Pessoa pessoa = Pessoa.fromFormularioPessoa(formulario);
        Pessoa criada = pessoaService.create(pessoa);
        return ResponseEntity.ok(criada);
    }
} 