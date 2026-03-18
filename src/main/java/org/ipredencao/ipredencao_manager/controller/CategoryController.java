package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.pessoa.AgregadorCategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/categorias")
public class CategoryController {

    @GetMapping("/agregadores")
    @PreAuthorize("hasAnyRole('BOLETIM', 'DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<List<AgregadorCategoriaEnum>> listCategoryAggregators() {
        return ResponseEntity.ok(Arrays.asList(AgregadorCategoriaEnum.values()));
    }

    @GetMapping("/agregadores/{idAgregador}")
    @PreAuthorize("hasAnyRole('BOLETIM', 'DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<List<CategoriaEnum>> listCategoriesByAggregator(@PathVariable Long idAgregador) {
        AgregadorCategoriaEnum agregador;
        try {
            agregador = AgregadorCategoriaEnum.fromId(idAgregador);
        } catch (IllegalArgumentException e) {
            throw new NoSuchElementException("Category aggregator not found with id: " + idAgregador);
        }
        return ResponseEntity.ok(Arrays.asList(CategoriaEnum.getByAgregadorCategoria(agregador)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('BOLETIM', 'DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<List<CategoriaEnum>> listAllCategories() {
        return ResponseEntity.ok(Arrays.asList(CategoriaEnum.values()));
    }
}
