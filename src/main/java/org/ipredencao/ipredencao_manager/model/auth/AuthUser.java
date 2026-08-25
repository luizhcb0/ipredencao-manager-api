package org.ipredencao.ipredencao_manager.model.auth;

import org.springframework.security.core.AuthenticatedPrincipal;

/** Principal do SecurityContext: o filtro JWT coloca isto; id, e-mail e perfil saem daqui. */
public record AuthUser(Long id, String email, PerfilAcesso profile) implements AuthenticatedPrincipal {

    /** Username da sessão no Spring; o login deste sistema é o e-mail. */
    @Override
    public String getName() {
        return email;
    }
}
