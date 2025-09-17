package org.ipredencao.ipredencao_manager.model.auth.dto;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;

public class RegisterRequest {
    
    private String name;
    private String email;
    private String password;
    private PerfilAcesso profile;
    
    public RegisterRequest() {}
    
    public RegisterRequest(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    public PerfilAcesso getProfile() { return profile; }

    public void setProfile(PerfilAcesso profile) { this.profile = profile; }
}