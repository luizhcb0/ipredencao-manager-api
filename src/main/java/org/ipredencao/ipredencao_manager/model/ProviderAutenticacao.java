package org.ipredencao.ipredencao_manager.model;

public enum ProviderAutenticacao {
    GOOGLE("Google OAuth"),
    FACEBOOK("Facebook OAuth"),
    EMAIL("Email/Senha");
    
    private final String descricao;
    
    ProviderAutenticacao(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
