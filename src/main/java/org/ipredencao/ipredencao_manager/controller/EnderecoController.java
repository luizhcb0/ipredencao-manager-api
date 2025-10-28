package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.ErrorResponse;
import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.endereco.EnderecoQuery;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.service.EnderecoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/enderecos")
public class EnderecoController {
    
    @Autowired
    private EnderecoService enderecoService;
    
    /**
     * Cria um novo endereço (com pessoaIds opcional)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> create(@RequestBody Endereco endereco) {
        try {
            Endereco created = enderecoService.create(endereco);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    /**
     * Atualiza um endereço e sincroniza pessoas vinculadas
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Endereco endereco) {
        try {
            endereco.setId(id);
            Endereco updated = enderecoService.update(endereco);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Endereço não encontrado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    /**
     * Deleta um endereço (apenas se não houver pessoas vinculadas)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            enderecoService.delete(id);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(new ErrorResponse("Endereço não encontrado"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    /**
     * Busca endereço por ID (incluindo pessoaIds)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            Endereco endereco = enderecoService.findById(id);
            if (endereco == null) {
                return ResponseEntity.status(404).body(new ErrorResponse("Endereço não encontrado"));
            }
            return ResponseEntity.ok(endereco);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro interno", e.getMessage()));
        }
    }
    
    /**
     * Busca endereços com filtros e paginação
     */
    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> search(@RequestBody EnderecoQuery query) {
        try {
            PagedResponse<Endereco> response = enderecoService.findPaginated(query);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse("Erro na busca", e.getMessage()));
        }
    }
}

