package org.ipredencao.ipredencao_manager.config;

import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Grupos de acesso derivados de {@link PerfilAcesso}. O bean expõe os mesmos grupos ao SpEL.
 */
@Component("roles")
public final class Roles {

    /** MEMBER e MEMBERSHIP_CANDIDATE ficam de fora de todos os grupos. */
    private static final PerfilAcesso[] ANY = {
        PerfilAcesso.BOLETIM, PerfilAcesso.DIACONO, PerfilAcesso.PRESBITERO, PerfilAcesso.ADMIN
    };
    private static final PerfilAcesso[] STAFF = {
        PerfilAcesso.DIACONO, PerfilAcesso.PRESBITERO, PerfilAcesso.ADMIN
    };
    private static final PerfilAcesso[] ELDER = {
        PerfilAcesso.PRESBITERO, PerfilAcesso.ADMIN
    };
    private static final PerfilAcesso[] ADMIN_ONLY = { PerfilAcesso.ADMIN };

    /** Leitura de pessoas, categorias, endereços, relatórios e serviços. */
    public static String[] anyNames() {
        return names(ANY);
    }

    /** DIACONO+: escrita de pessoa/endereço/serviço, filtro de pendência e leitura de formulários. */
    public static String[] staffNames() {
        return names(STAFF);
    }

    /** PRESBITERO+: atos oficiais e gestação em sigilo. */
    public static String[] elderNames() {
        return names(ELDER);
    }

    /** ADMIN: usuários, actuator e exclusão de endereço. */
    public static String[] adminNames() {
        return names(ADMIN_ONLY);
    }

    public static final String ANY_ROLE_EXPR = "@roles.any(authentication)";
    public static final String STAFF_EXPR = "@roles.staff(authentication)";
    public static final String ELDER_EXPR = "@roles.elder(authentication)";
    public static final String ADMIN_EXPR = "@roles.admin(authentication)";

    public boolean any(Authentication authentication) {
        return SecurityUtils.hasAnyRole(authentication, anyNames());
    }

    public boolean staff(Authentication authentication) {
        return SecurityUtils.hasAnyRole(authentication, staffNames());
    }

    public boolean elder(Authentication authentication) {
        return SecurityUtils.hasAnyRole(authentication, elderNames());
    }

    public boolean admin(Authentication authentication) {
        return SecurityUtils.hasAnyRole(authentication, adminNames());
    }

    private static String[] names(PerfilAcesso[] profiles) {
        return Arrays.stream(profiles).map(profile -> profile.name()).toArray(String[]::new);
    }
}
