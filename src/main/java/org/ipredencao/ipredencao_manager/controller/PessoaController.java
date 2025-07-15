package org.ipredencao.ipredencao_manager.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.model.RelacionamentoPessoa;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/pessoas")
public class PessoaController {
    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {this.pessoaService = pessoaService;}

    @PostMapping
    public Pessoa criarPessoa(@RequestBody Pessoa pessoa) {
        return pessoaService.criarPessoa(pessoa);
    }

    @PostMapping("/{id}/foto")
    public ResponseEntity<Pessoa> uploadFoto(
        @PathVariable Long id,
        @RequestParam("foto") MultipartFile foto
    ) throws IOException {
        Pessoa pessoa = pessoaService.buscarPorId(id);
        if (pessoa == null) {
            return ResponseEntity.notFound().build();
        }
        pessoaService.criarPessoaComFoto(id, foto);
        Pessoa atualizada = pessoaService.buscarPorId(id);
        return ResponseEntity.ok(atualizada);
    }

    @GetMapping("/{id}")
    public Pessoa buscarPorId(@PathVariable Long id) {
        return pessoaService.buscarPorId(id);
    }

    @GetMapping
    public List<Pessoa> listarTodas() {
        return pessoaService.listarTodas();
    }

    @PutMapping("/{id}")
    public int atualizarPessoa(@PathVariable Long id, @RequestBody Pessoa pessoa) {
        pessoa.setId(id);
        return pessoaService.atualizarPessoa(pessoa);
    }

    @DeleteMapping("/{id}")
    public int deletarPessoa(@PathVariable Long id) {
        return pessoaService.deletarPessoa(id);
    }

    // Relacionamentos qualificados
    @PostMapping("/{id}/relacionamentos")
    public RelacionamentoPessoa criarRelacionamento(@PathVariable Long id, @RequestBody RelacionamentoPessoa relacionamento) {
        return pessoaService.criarRelacionamento(id, relacionamento);
    }

    @GetMapping("/{id}/relacionamentos")
    public List<RelacionamentoPessoa> listarRelacionamentos(@PathVariable Long id) {
        return pessoaService.listarRelacionamentosPorPessoa(id);
    }

    @DeleteMapping("/relacionamentos/{relacionamentoId}")
    public int deletarRelacionamento(@PathVariable Long relacionamentoId) {
        return pessoaService.deletarRelacionamento(relacionamentoId);
    }
} 