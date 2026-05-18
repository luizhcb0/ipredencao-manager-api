package org.ipredencao.ipredencao_manager.repository;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.List;

/**
 * Helpers para construção de condições de busca usadas pelos repositórios.
 * Centraliza padrões repetidos como busca textual com unaccent e igualdade
 * condicional para strings não vazias.
 */
public final class QueryConditions {

    private QueryConditions() {}

    /**
     * Compara coluna textual com o valor informado ignorando acentos e case.
     * Equivalente a: LOWER(unaccent(column)) LIKE LOWER(unaccent('%value%')).
     */
    public static Condition unaccentedLike(Field<String> column, String value) {
        return DSL.lower(DSL.function("unaccent", String.class, column))
            .like(DSL.lower(DSL.function("unaccent", String.class, DSL.inline("%" + value + "%"))));
    }

    /**
     * Adiciona {@link #unaccentedLike} à lista somente se o valor não for nulo nem em branco.
     */
    public static void addUnaccentedLike(List<Condition> conditions, Field<String> column, String value) {
        if (value != null && !value.trim().isEmpty()) {
            conditions.add(unaccentedLike(column, value));
        }
    }

    /**
     * Adiciona condição {@code column.eq(value)} à lista somente se o valor não for nulo nem em branco.
     */
    public static void addEqIfNotBlank(List<Condition> conditions, Field<String> column, String value) {
        if (value != null && !value.trim().isEmpty()) {
            conditions.add(column.eq(value));
        }
    }

    /**
     * Reduz uma lista de condições com AND, retornando {@link DSL#noCondition()} se vazia.
     */
    public static Condition reduceToAnd(List<Condition> conditions) {
        return conditions.stream().reduce(DSL.noCondition(), Condition::and);
    }
}
