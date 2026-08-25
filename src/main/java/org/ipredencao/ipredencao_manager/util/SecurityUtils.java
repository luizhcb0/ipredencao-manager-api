package org.ipredencao.ipredencao_manager.util;

import org.ipredencao.ipredencao_manager.model.auth.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * Mesma fonte dos matchers e do {@code @PreAuthorize}: a authority {@code ROLE_<perfil>}.
     * Sem {@code Authentication} retorna {@code false}. Os grupos estão em {@code Roles}.
     */
    public static boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || roles == null || roles.length == 0) return false;
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            String authority = granted.getAuthority();
            for (String role : roles) {
                if (authority.equals("ROLE_" + role)) return true;
            }
        }
        return false;
    }

    /** Id do {@link AuthUser} no contexto; {@code null} se o principal não for o do filtro JWT. */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        if (authentication.getPrincipal() instanceof AuthUser user) return user.id();
        return null;
    }

    /**
     * Extrai o email do usuário atual do SecurityContext
     * @return Email do usuário ou null se não autenticado
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Verifica se existe um usuário autenticado
     * @return true se autenticado, false caso contrário
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() &&
               !"anonymousUser".equals(authentication.getName());
    }
}
