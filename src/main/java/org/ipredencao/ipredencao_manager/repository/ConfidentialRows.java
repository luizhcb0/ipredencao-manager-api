package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.config.Roles;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.ConfidentialAccess;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.jooq.Condition;
import org.jooq.Field;

/**
 * Sigilo de gestação (categoria 30). Toda query que expuser id ou nome de {@code pessoa}
 * precisa passar por aqui — o filtro do {@code PessoaRepository} não alcança quem lê a
 * tabela por conta própria.
 */
public final class ConfidentialRows {

    private static final Long CATEGORIA_ID = CategoriaEnum.GESTACAO_SIGILO_TEMPORARIO.getId();

    private ConfidentialRows() {}

    public static boolean canSee(ConfidentialAccess access) {
        return access == ConfidentialAccess.INTERNAL || SecurityUtils.hasAnyRole(Roles.elderNames());
    }

    public static boolean isConfidential(Long categoriaId) {
        return CATEGORIA_ID.equals(categoriaId);
    }

    /** Passe o campo do alias em uso: a mesma query pode juntar {@code pessoa} mais de uma vez. */
    public static Condition notConfidential(Field<Long> categoriaId) {
        return categoriaId.ne(CATEGORIA_ID);
    }
}
