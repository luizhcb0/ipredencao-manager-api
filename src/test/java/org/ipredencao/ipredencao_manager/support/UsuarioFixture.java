package org.ipredencao.ipredencao_manager.support;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.auth.ProviderAutenticacao;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.joda.time.DateTime;

public final class UsuarioFixture {

    private UsuarioFixture() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String firebaseUid = "firebase-uid-" + System.nanoTime();
        private String email = "user-" + System.nanoTime() + "@test.local";
        private String name = "Usuario Teste";
        private PerfilAcesso accessProfile = PerfilAcesso.BOLETIM;
        private ProviderAutenticacao provider = ProviderAutenticacao.EMAIL;
        private Boolean active = true;

        public Builder firebaseUid(String firebaseUid) {
            this.firebaseUid = firebaseUid;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder accessProfile(PerfilAcesso accessProfile) {
            this.accessProfile = accessProfile;
            return this;
        }

        public Builder provider(ProviderAutenticacao provider) {
            this.provider = provider;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        public Usuario build() {
            Usuario usuario = new Usuario();
            usuario.setFirebaseUid(firebaseUid);
            usuario.setEmail(email);
            usuario.setName(name);
            usuario.setAccessProfile(accessProfile);
            usuario.setProvider(provider);
            usuario.setAddedAt(DateTime.now());
            usuario.setActive(active);
            return usuario;
        }
    }
}
