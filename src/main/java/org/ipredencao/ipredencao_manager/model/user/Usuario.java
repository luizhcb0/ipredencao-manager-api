package org.ipredencao.ipredencao_manager.model.user;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.auth.ProviderAutenticacao;
import org.joda.time.DateTime;

// Validações removidas temporariamente para simplificar

public class Usuario {
    
    private Long id;
    
    private String firebaseUid;
    
    private String email;
    
    private String name;
    
    private PerfilAcesso accessProfile = PerfilAcesso.BOLETIM;
    
    private DateTime addedAt;
    
    private DateTime lastLogin;
    
    private DateTime updatedAt;
    
    private Boolean active = true;
    
    private ProviderAutenticacao provider;
    
    private Integer failedLoginAttempts = 0;
    
    private DateTime blockedUntil;
    
    // Métodos de conveniência
    public boolean isContaBloqueada() {
        return blockedUntil != null && blockedUntil.isAfterNow();
    }
    
    public void incrementarTentativasFalhou() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.blockedUntil = DateTime.now().plusMinutes(30);
        }
    }
    
    public void resetarTentativasFalhou() {
        this.failedLoginAttempts = 0;
        this.blockedUntil = null;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFirebaseUid() {
        return firebaseUid;
    }
    
    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
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
    
    public DateTime getAddedAt() {
        return addedAt;
    }
    
    public void setAddedAt(DateTime addedAt) {
        this.addedAt = addedAt;
    }
    
    public DateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(DateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public DateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(DateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public ProviderAutenticacao getProvider() {
        return provider;
    }
    
    public void setProvider(ProviderAutenticacao provider) {
        this.provider = provider;
    }
    
    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }
    
    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }
    
    public DateTime getBlockedUntil() {
        return blockedUntil;
    }
    
    public void setBlockedUntil(DateTime blockedUntil) {
        this.blockedUntil = blockedUntil;
    }
}
