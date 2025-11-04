package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.pessoa.AgregadorCategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin
public class CategoryController {
    
    /**
     * Lista todos os agregadores de categorias disponíveis no sistema
     * @return Lista de agregadores de categorias com id e nome
     */
    @GetMapping("/agregadores")
    @PreAuthorize("hasAnyRole('BOLETIM', 'DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> listCategoryAggregators() {
        try {
            List<AgregadorCategoriaEnum> aggregators = Arrays.asList(AgregadorCategoriaEnum.values());
            
            return ResponseEntity.ok(aggregators);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro ao listar agregadores de categorias", e.getMessage()));
        }
    }
    
    /**
     * Lista todas as categorias de um agregador específico
     * @param idAgregador ID do agregador de categoria
     * @return Lista de categorias do agregador informado
     */
    @GetMapping("/agregadores/{idAgregador}")
    @PreAuthorize("hasAnyRole('BOLETIM', 'DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> listCategoriesByAggregator(@PathVariable Long idAgregador) {
        try {
            // Buscar o agregador pelo ID
            AgregadorCategoriaEnum agregador;
            try {
                agregador = AgregadorCategoriaEnum.fromId(idAgregador);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Agregador de categoria não encontrado", "ID de agregador inválido: " + idAgregador));
            }
            
            // Buscar categorias do agregador
            CategoriaEnum[] categories = CategoriaEnum.getByAgregadorCategoria(agregador);
            
            List<CategoriaEnum> result = Arrays.asList(categories);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro ao listar categorias", e.getMessage()));
        }
    }
    
    /**
     * Lista todas as categorias disponíveis no sistema
     * @return Lista de todas as categorias
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('BOLETIM', 'DIACONO', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> listAllCategories() {
        try {
            List<CategoriaEnum> categories = Arrays.asList(CategoriaEnum.values());
            
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro ao listar categorias", e.getMessage()));
        }
    }
}
