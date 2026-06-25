package org.ipredencao.ipredencao_manager.model.user.dto;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;

public class UpdateUserRequest {

    private String name;
    private PerfilAcesso accessProfile;
    private Boolean active;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PerfilAcesso getAccessProfile() {
        return accessProfile;
    }

    public void setAccessProfile(PerfilAcesso accessProfile) {
        this.accessProfile = accessProfile;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
