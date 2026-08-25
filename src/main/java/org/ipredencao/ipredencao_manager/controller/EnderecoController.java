package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.config.Roles;
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

    @PostMapping
    @PreAuthorize(Roles.STAFF_EXPR)
    public ResponseEntity<Endereco> create(@RequestBody Endereco endereco) {
        return ResponseEntity.ok(enderecoService.create(endereco));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.STAFF_EXPR)
    public ResponseEntity<Endereco> update(@PathVariable Long id, @RequestBody Endereco endereco) {
        endereco.setId(id);
        return ResponseEntity.ok(enderecoService.update(endereco));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Roles.ADMIN_EXPR)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        enderecoService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Roles.ANY_ROLE_EXPR)
    public ResponseEntity<Endereco> findById(@PathVariable Long id) {
        Endereco endereco = enderecoService.findById(id);
        if (endereco == null) {
            throw new NoSuchElementException("Address not found with id: " + id);
        }
        return ResponseEntity.ok(endereco);
    }

    @PostMapping("/search")
    @PreAuthorize(Roles.ANY_ROLE_EXPR)
    public ResponseEntity<PagedResponse<Endereco>> search(@RequestBody EnderecoQuery query) {
        return ResponseEntity.ok(enderecoService.findPaginated(query));
    }
}
