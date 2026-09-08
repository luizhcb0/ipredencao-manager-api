package org.ipredencao.ipredencao_manager.model.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;

public class UpdateUserRequest {

    private String name;
    private PerfilAcesso accessProfile;
    private Boolean active;
    private Long personId;
    private boolean personIdPresent;

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

    public Long getPersonId() {
        return personId;
    }

    @JsonSetter("personId")
    public void setPersonId(Long personId) {
        this.personId = personId;
        this.personIdPresent = true;
    }

    @JsonIgnore
    public boolean isPersonIdPresent() {
        return personIdPresent;
    }
}
