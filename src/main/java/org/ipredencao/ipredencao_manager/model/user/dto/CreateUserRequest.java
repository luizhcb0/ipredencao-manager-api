package org.ipredencao.ipredencao_manager.model.user.dto;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;

public record CreateUserRequest(
    String name,
    String email,
    PerfilAcesso profile,
    Long personId
) {}
