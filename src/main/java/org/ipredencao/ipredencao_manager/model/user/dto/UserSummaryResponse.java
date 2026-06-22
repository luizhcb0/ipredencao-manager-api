package org.ipredencao.ipredencao_manager.model.user.dto;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.auth.ProviderAutenticacao;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.joda.time.DateTime;

public class UserSummaryResponse {

    private Long id;
    private String name;
    private String email;
    private PerfilAcesso accessProfile;
    private Boolean canResendInvite;
    private Boolean active;
    private String lastLogin;
    private String addedAt;

    public static UserSummaryResponse from(Usuario usuario) {
        UserSummaryResponse response = new UserSummaryResponse();
        response.setId(usuario.getId());
        response.setName(usuario.getName());
        response.setEmail(usuario.getEmail());
        response.setAccessProfile(usuario.getAccessProfile());
        response.setCanResendInvite(
            usuario.getProvider() == null || usuario.getProvider() == ProviderAutenticacao.EMAIL
        );
        response.setActive(usuario.getActive());
        response.setLastLogin(formatDateTime(usuario.getLastLogin()));
        response.setAddedAt(formatDateTime(usuario.getAddedAt()));
        return response;
    }

    private static String formatDateTime(DateTime dateTime) {
        return dateTime != null ? dateTime.toString() : null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public PerfilAcesso getAccessProfile() {
        return accessProfile;
    }

    public void setAccessProfile(PerfilAcesso accessProfile) {
        this.accessProfile = accessProfile;
    }

    public Boolean getCanResendInvite() {
        return canResendInvite;
    }

    public void setCanResendInvite(Boolean canResendInvite) {
        this.canResendInvite = canResendInvite;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(String lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(String addedAt) {
        this.addedAt = addedAt;
    }
}
