package org.ipredencao.ipredencao_manager.config;

/**
 * Grupos de perfis nas duas formas que a autorização consome: {@code String[]} para os
 * matchers e SpEL para {@code @PreAuthorize}, que exige constante de compilação.
 */
public final class Roles {

    public static final String BOLETIM = "BOLETIM";
    public static final String DIACONO = "DIACONO";
    public static final String PRESBITERO = "PRESBITERO";
    public static final String ADMIN = "ADMIN";

    /** Todos os perfis: leitura de pessoas, categorias, endereços, relatórios e serviços. */
    public static final String[] ANY_ROLE = { BOLETIM, DIACONO, PRESBITERO, ADMIN };

    /** DIACONO+: escrita de pessoa/endereço/serviço, filtro de pendência e leitura de formulários. */
    public static final String[] STAFF = { DIACONO, PRESBITERO, ADMIN };

    /** PRESBITERO+: atos oficiais e gestação em sigilo. */
    public static final String[] ELDER = { PRESBITERO, ADMIN };

    /** ADMIN: usuários, actuator e exclusão de endereço. */
    public static final String[] ADMIN_ONLY = { ADMIN };

    public static final String ANY_ROLE_EXPR =
            "hasAnyRole('" + BOLETIM + "','" + DIACONO + "','" + PRESBITERO + "','" + ADMIN + "')";
    public static final String STAFF_EXPR =
            "hasAnyRole('" + DIACONO + "','" + PRESBITERO + "','" + ADMIN + "')";
    public static final String ELDER_EXPR =
            "hasAnyRole('" + PRESBITERO + "','" + ADMIN + "')";
    public static final String ADMIN_EXPR =
            "hasRole('" + ADMIN + "')";

    private Roles() {
    }
}
