package org.ipredencao.ipredencao_manager.model;

import java.util.Set;

public enum PerfilAcesso {
    BOLETIM("Usuário Boletim", Set.of("READ")),
    PRESBITERO("Presbítero", Set.of("READ", "WRITE", "UPDATE")),
    ADMIN("Administrador", Set.of("READ", "WRITE", "UPDATE", "DELETE", "MANAGE"));
    
    private final String descricao;
    private final Set<String> permissoes;
    
    PerfilAcesso(String descricao, Set<String> permissoes) {
        this.descricao = descricao;
        this.permissoes = permissoes;
    }
    
    public boolean temPermissao(String permissao) {
        return permissoes.contains(permissao);
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public Set<String> getPermissoes() {
        return permissoes;
    }
}
