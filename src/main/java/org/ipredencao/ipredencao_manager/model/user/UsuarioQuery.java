package org.ipredencao.ipredencao_manager.model.user;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.auth.ProviderAutenticacao;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.model.pagination.SortDirection;

public class UsuarioQuery {
    public enum Sort { LAST_LOGIN, ACTIVE }

    private Long id;
    private String email;
    private String firebaseUid;
    private String name;
    private PerfilAcesso accessProfile;
    private Boolean active;
    private ProviderAutenticacao provider;
    private Long personId;
    private PaginationParameters pagination;
    private Sort sort;
    private SortDirection dir;

    public UsuarioQuery() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final UsuarioQuery query = new UsuarioQuery();

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

        public Builder name(String name) {
            query.name = name;
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

        public Builder personId(Long personId) {
            query.personId = personId;
            return this;
        }

        public Builder pagination(PaginationParameters pagination) {
            query.pagination = pagination;
            return this;
        }

        public Builder sort(Sort sort) {
            query.sort = sort;
            return this;
        }

        public Builder dir(SortDirection dir) {
            query.dir = dir;
            return this;
        }

        public UsuarioQuery build() {
            return query;
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getName() {
        return name;
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

    public Long getPersonId() {
        return personId;
    }

    public PaginationParameters getPagination() {
        return pagination;
    }

    public Sort getSort() {
        return sort;
    }

    public SortDirection getDir() {
        return dir;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAccessProfile(PerfilAcesso accessProfile) {
        this.accessProfile = accessProfile;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setProvider(ProviderAutenticacao provider) {
        this.provider = provider;
    }

    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    public void setPagination(PaginationParameters pagination) {
        this.pagination = pagination;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    public void setDir(SortDirection dir) {
        this.dir = dir;
    }
}
