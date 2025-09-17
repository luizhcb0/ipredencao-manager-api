package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.SubcategoriaEnum;
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
     * Lista todas as categorias disponíveis no sistema
     * @return Lista de categorias com código e nome
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> listarCategorias() {
        try {
            List<CategoriaEnum> categorias = Arrays.asList(CategoriaEnum.values());
            
            return ResponseEntity.ok(categorias);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro ao listar categorias", e.getMessage()));
        }
    }
    
    /**
     * Lista todas as subcategorias de uma categoria específica
     * @param codigoCategoria Código da categoria (ex: "01", "02", etc)
     * @return Lista de subcategorias da categoria informada
     */
    @GetMapping("/{codigoCategoria}/subcategorias")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> listarSubcategoriasPorCategoria(@PathVariable String codigoCategoria) {
        try {
            // Buscar a categoria pelo código
            CategoriaEnum categoria;
            try {
                categoria = CategoriaEnum.fromCodigo(codigoCategoria);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Categoria não encontrada", "Código de categoria inválido: " + codigoCategoria));
            }
            
            // Buscar subcategorias da categoria
            SubcategoriaEnum[] subcategorias = SubcategoriaEnum.getByCategoria(categoria);
            
            List<SubcategoriaEnum> resultado = Arrays.asList(subcategorias);
            
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro ao listar subcategorias", e.getMessage()));
        }
    }
    
    /**
     * Lista todas as subcategorias disponíveis no sistema
     * @return Lista de todas as subcategorias
     */
    @GetMapping("/subcategorias")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> listarTodasSubcategorias() {
        try {
            List<SubcategoriaEnum> subcategorias = Arrays.asList(SubcategoriaEnum.values());
            
            return ResponseEntity.ok(subcategorias);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro ao listar subcategorias", e.getMessage()));
        }
    }
}
