package org.ipredencao.ipredencao_manager.model.auth;

public class UserProfile {
    
    private Long id;
    private String email;
    private String nome;
    private String fotoUrl;
    private String perfilAcesso;
    private String dataUltimoLogin;
    
    public UserProfile() {}
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getNome() {
        return nome;
    }
    
    public void setNome(String nome) {
        this.nome = nome;
    }
    
    public String getFotoUrl() {
        return fotoUrl;
    }
    
    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }
    
    public String getPerfilAcesso() {
        return perfilAcesso;
    }
    
    public void setPerfilAcesso(String perfilAcesso) {
        this.perfilAcesso = perfilAcesso;
    }
    
    public String getDataUltimoLogin() {
        return dataUltimoLogin;
    }
    
    public void setDataUltimoLogin(String dataUltimoLogin) {
        this.dataUltimoLogin = dataUltimoLogin;
    }
}