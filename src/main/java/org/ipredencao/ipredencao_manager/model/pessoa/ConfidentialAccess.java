package org.ipredencao.ipredencao_manager.model.pessoa;

/**
 * Escopo de uma leitura de pessoa quanto à gestação em sigilo (categoria 30). É
 * argumento, e não campo da query, para o cliente não ter como pedir {@code INTERNAL}.
 */
public enum ConfidentialAccess {

    /** Filtra pelo perfil de quem chamou. */
    CALLER,

    /** Leitura interna do servidor: enxerga sigilo independente do perfil. */
    INTERNAL
}
