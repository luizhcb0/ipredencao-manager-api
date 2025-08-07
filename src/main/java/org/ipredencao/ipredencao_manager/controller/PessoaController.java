package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.relacionamento_pessoa.RelacionamentoPessoaIds;
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
@RequestMapping("/pessoas")
public class PessoaController {
    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {this.pessoaService = pessoaService;}

    @PostMapping
    public ResponseEntity<Pessoa> criarPessoa(@RequestBody Pessoa pessoa) {
        return ResponseEntity.ok(pessoaService.create(pessoa));
    }

    @PostMapping("/{id}/foto")
    public ResponseEntity<Pessoa> uploadPhoto(
        @PathVariable Long id,
        @RequestParam("foto") MultipartFile foto
    ) throws IOException {
        Pessoa pessoa = pessoaService.findById(id);
        if (pessoa == null) {
            return ResponseEntity.notFound().build();
        }
        Pessoa updated = pessoaService.savePhoto(pessoa, foto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable Long id) {
        try {
            Pessoa pessoa = pessoaService.findById(id);
            return ResponseEntity.ok(pessoa);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/search")
    public ResponseEntity<List<Pessoa>> buscarPessoas(@RequestBody PessoaQuery query) {
        List<Pessoa> pessoas = pessoaService.find(query);
        return ResponseEntity.ok(pessoas);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Pessoa> atualizarPessoa(@PathVariable Long id, @RequestBody Pessoa pessoa) {
        pessoa.setId(id);
        return ResponseEntity.ok(pessoaService.update(pessoa));
    }

    // Relacionamentos qualificados
    @PostMapping("/{id}/relacionamentos")
    public RelacionamentoPessoa criarRelacionamento(@PathVariable Long id, @RequestBody RelacionamentoPessoaIds relacionamento) {
        return pessoaService.criarRelacionamento(id, relacionamento);
    }

    @GetMapping("/{id}/relacionamentos")
    public List<RelacionamentoPessoa> listarRelacionamentos(@PathVariable Long id) {
        return pessoaService.listarRelacionamentosPorPessoa(id);
    }
    
    /**
     * Lista todas as subcategorias disponíveis
     */
    @GetMapping("/subcategorias")
    public ResponseEntity<SubcategoriaEnum[]> getAllSubcategorias() {
        return ResponseEntity.ok(pessoaService.getAllSubcategorias());
    }
}
