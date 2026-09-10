package org.ipredencao.ipredencao_manager.config;

import org.ipredencao.ipredencao_manager.model.auth.AuthUser;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollmentQuery;
import org.ipredencao.ipredencao_manager.model.ebd.EbdEnrollmentRoleEnum;
import org.ipredencao.ipredencao_manager.repository.EbdEnrollmentRepository;
import org.ipredencao.ipredencao_manager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;

/**
 * Autorização data-driven da EBD: "esta pessoa é professora desta turma?" não é
 * um {@code perfil_acesso} (ver docs/EBD_ANALISE_E_PLANO.md B.3) — é um vínculo
 * ({@code ebd_enrollment.role = TEACHER}). Uso: {@code @PreAuthorize("@roles.staff(authentication)
 * or @ebdAccess.isTeacherOf(authentication, #id)")}.
 */
@Component("ebdAccess")
public class EbdAccess {

    @Autowired
    private UserService userService;

    @Autowired
    private EbdEnrollmentRepository enrollmentRepository;

    public boolean isTeacherOf(Authentication authentication, Long classId) {
        Long personId = currentPersonId(authentication);
        if (personId == null || classId == null) return false;
        return !enrollmentRepository.find(EbdEnrollmentQuery.builder()
                .classId(classId)
                .personId(personId)
                .role(EbdEnrollmentRoleEnum.TEACHER)
                .build()).isEmpty();
    }

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
