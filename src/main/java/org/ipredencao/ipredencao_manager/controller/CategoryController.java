package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.SubcategoriaEnum;
import org.ipredencao.ipredencao_manager.model.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categorias")
@CrossOrigin
public class CategoryController {
    
    /**
     * Lista todas as categorias disponíveis no sistema
     * @return Lista de nomes das categorias
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> listarCategorias() {
        try {
            List<String> categorias = Arrays.stream(CategoriaEnum.values())
                .map(CategoriaEnum::getNome)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(categorias);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro ao listar categorias", e.getMessage()));
        }
    }
    
    /**
     * Lista todas as subcategorias de uma categoria específica
     * @param codigoCategoria Código da categoria (ex: "01", "02", etc)
     * @return Lista de nomes das subcategorias da categoria informada
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
            
            List<String> resultado = Arrays.stream(subcategorias)
                .map(SubcategoriaEnum::getNome)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro ao listar subcategorias", e.getMessage()));
        }
    }
    
    /**
     * Lista todas as subcategorias disponíveis no sistema
     * @return Lista de nomes de todas as subcategorias
     */
    @GetMapping("/subcategorias")
    @PreAuthorize("hasAnyRole('BOLETIM', 'PRESBITERO', 'ADMIN')")
    public ResponseEntity<?> listarTodasSubcategorias() {
        try {
            List<String> subcategorias = Arrays.stream(SubcategoriaEnum.values())
                .map(SubcategoriaEnum::getNome)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(subcategorias);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(new ErrorResponse("Erro ao listar subcategorias", e.getMessage()));
        }
    }
}
