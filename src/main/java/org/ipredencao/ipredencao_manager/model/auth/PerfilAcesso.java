package org.ipredencao.ipredencao_manager.model.auth;

/** A autorização real é a authority {@code ROLE_<perfil>}; os grupos ficam em {@code config.Roles}. */
public enum PerfilAcesso {
    BOLETIM,
    DIACONO,
    PRESBITERO,
    ADMIN
}
