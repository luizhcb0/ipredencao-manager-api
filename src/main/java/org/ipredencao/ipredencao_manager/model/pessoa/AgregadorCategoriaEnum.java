package org.ipredencao.ipredencao_manager.model.pessoa;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum AgregadorCategoriaEnum {
    PASTOR(1L, "Pastor"),
    MEMBRO_COMUNGANTE(2L, "Membro comungante"),
    MEMBRO_NAO_COMUNGANTE(3L, "Membro não comungante"),
    ROL_A_PARTE(4L, "Rol à parte"),
    ADMITENDO(5L, "Admitendo"),
    TRANSFERIDO(6L, "Transferido"),
    EXCLUIDO(7L, "Excluido"),
    MISSIONARIO(8L, "Missionário"),
    POSSIVEL_ADMISSAO_GESTACAO(9L, "Possível admissão: gestação"),
    PESSOA_REFERENCIADA(10L, "Pessoa referenciada");
    
    private final Long id;
    private final String nome;
    
    AgregadorCategoriaEnum(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }
    
    public Long getId() {
        return id;
    }
    
    public String getNome() {
        return nome;
    }
    
    public static AgregadorCategoriaEnum fromId(Long id) {
        for (AgregadorCategoriaEnum categoria : values()) {
            if (categoria.id.equals(id)) {
                return categoria;
            }
        }
        throw new IllegalArgumentException("Agregador de categoria não encontrado para o ID: " + id);
    }
    
    public static AgregadorCategoriaEnum fromName(String nome) {
        for (AgregadorCategoriaEnum categoria : values()) {
            if (categoria.nome.equals(nome)) {
                return categoria;
            }
        }
        throw new IllegalArgumentException("Agregador de categoria não encontrado para o nome: " + nome);
    }
}
