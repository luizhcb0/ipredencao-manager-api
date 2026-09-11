package org.ipredencao.ipredencao_manager.config;

import org.ipredencao.ipredencao_manager.model.auth.AuthUser;
import org.ipredencao.ipredencao_manager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

/**
 * Resolve a identidade do chamador dentro do domínio da EBD. Chegou a expor
 * também {@code isTeacherOf(authentication, classId)} (autorização data-driven:
 * "esta pessoa é professora desta turma?", via {@code ebd_enrollment.role =
 * TEACHER}), removido a pedido do time em revisão do PR — de início a permissão
 * de gerenciar uma turma fica só com STAFF (diácono/presbítero/admin), sem essa
 * granularidade por vínculo. O papel {@code TEACHER} em si continua existindo
 * em {@code ebd_enrollment}, mas hoje é só um rótulo informativo — não concede
 * permissão nem condiciona nenhuma outra regra.
 */
@Component("ebdAccess")
public class EbdAccess {

    @Autowired
    private UserService userService;

    /**
     * Resolve o {@code usuario.person_id} do chamador — vínculo já existente
     * (V014), fora do escopo da EBD; ver {@code UserService.getUser}. Também
     * usado por {@code EbdService} para as ações de automatrícula/presença
     * (POST .../enrollments/me), que não passam por {@code @PreAuthorize} com
     * classId porque ainda não existe matrícula no momento da checagem.
     */
    public Long currentPersonId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) return null;
        try {
            return userService.getUser(user.id()).personId();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
