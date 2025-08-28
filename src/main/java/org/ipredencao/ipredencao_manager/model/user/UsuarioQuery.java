package org.ipredencao.ipredencao_manager.model.user;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.auth.ProviderAutenticacao;

public class UsuarioQuery {
    private Long id;
    private String email;
    private String firebaseUid;
    private PerfilAcesso accessProfile;
    private Boolean active;
    private ProviderAutenticacao provider;
    
    // Construtor privado para forçar uso do builder
    private UsuarioQuery() {}
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UsuarioQuery query = new UsuarioQuery();
        
        public Builder id(Long id) {
            query.id = id;
            return this;
        }
        
        public Builder email(String email) {
            query.email = email;
            return this;
        }
        
        public Builder firebaseUid(String firebaseUid) {
            query.firebaseUid = firebaseUid;
            return this;
        }
        
        public Builder accessProfile(PerfilAcesso accessProfile) {
            query.accessProfile = accessProfile;
            return this;
        }
        
        public Builder active(Boolean active) {
            query.active = active;
            return this;
        }
        
        public Builder provider(ProviderAutenticacao provider) {
            query.provider = provider;
            return this;
        }
        
        public UsuarioQuery build() {
            return query;
        }
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getFirebaseUid() {
        return firebaseUid;
    }
    
    public PerfilAcesso getAccessProfile() {
        return accessProfile;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public ProviderAutenticacao getProvider() {
        return provider;
    }
}
