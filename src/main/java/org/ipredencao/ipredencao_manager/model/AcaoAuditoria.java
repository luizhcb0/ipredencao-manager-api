package org.ipredencao.ipredencao_manager.model;

public enum AcaoAuditoria {
    LOGIN("Login realizado"),
    LOGOUT("Logout realizado"),
    REGISTER("Usuário registrado"),
    CREATE_FORMULARIO("Formulário criado"),
    UPDATE_FORMULARIO("Formulário atualizado"),
    DELETE_FORMULARIO("Formulário excluído"),
    CREATE_PESSOA("Pessoa criada"),
    UPDATE_PESSOA("Pessoa atualizada"),
    DELETE_PESSOA("Pessoa excluída"),
    CHANGE_PROFILE("Perfil alterado"),
    RESET_PASSWORD("Senha redefinida");
    
    private final String descricao;
    
    AcaoAuditoria(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
}
