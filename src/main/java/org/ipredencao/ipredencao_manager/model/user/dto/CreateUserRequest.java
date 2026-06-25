package org.ipredencao.ipredencao_manager.model.user.dto;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;

public class CreateUserRequest {

    private String name;
    private String email;
    private PerfilAcesso profile;

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

    public PerfilAcesso getProfile() {
        return profile;
    }

    public void setProfile(PerfilAcesso profile) {
        this.profile = profile;
    }
}
