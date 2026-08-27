package org.ipredencao.ipredencao_manager.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RolesTest {

    @Test
    void expressionsDelegateToRolesBean() {
        assertThat(Roles.ANY_ROLE_EXPR).isEqualTo("@roles.any(authentication)");
        assertThat(Roles.STAFF_EXPR).isEqualTo("@roles.staff(authentication)");
        assertThat(Roles.ELDER_EXPR).isEqualTo("@roles.elder(authentication)");
        assertThat(Roles.ADMIN_EXPR).isEqualTo("@roles.admin(authentication)");
    }

    @Test
    void domainProfilesMatchDatabaseEnum() {
        Set<String> domainProfiles = Arrays.stream(
                org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso.values())
            .map(profile -> profile.name())
            .collect(Collectors.toSet());
        Set<String> databaseProfiles = Arrays.stream(
                org.ipredencao.ipredencao_manager.jooq.enums.PerfilAcesso.values())
            .map(profile -> profile.getLiteral())
            .collect(Collectors.toSet());

        assertThat(domainProfiles).isEqualTo(databaseProfiles);
    }
}
