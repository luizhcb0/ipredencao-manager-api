package org.ipredencao.ipredencao_manager.model.user.dto;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.auth.ProviderAutenticacao;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.joda.time.DateTime;

public record UserSummaryResponse(
    Long id,
    String name,
    String email,
    PerfilAcesso accessProfile,
    Boolean canResendInvite,
    Boolean active,
    String lastLogin,
    String addedAt,
    Long personId
) {
    public static UserSummaryResponse from(Usuario usuario) {
        return new UserSummaryResponse(
            usuario.getId(),
            usuario.getName(),
            usuario.getEmail(),
            usuario.getAccessProfile(),
            usuario.getProvider() == null || usuario.getProvider() == ProviderAutenticacao.EMAIL,
            usuario.getActive(),
            formatDateTime(usuario.getLastLogin()),
            formatDateTime(usuario.getAddedAt()),
            usuario.getPersonId()
        );
    }

    private static String formatDateTime(DateTime dateTime) {
        return dateTime != null ? dateTime.toString() : null;
    }
}
