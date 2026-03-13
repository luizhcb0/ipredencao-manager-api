package org.ipredencao.ipredencao_manager.service.firebase;

/**
 * Response da Lambda Firebase Proxy
 */
public record FirebaseLambdaResponse(
    boolean success,
    ResponseData data,
    String error
) {
    /**
     * Dados do response - campos preenchidos conforme a operacao
     */
    public record ResponseData(
        String uid,
        String email,
        String name,
        String displayName,
        Boolean emailVerified,
        String token
    ) {}
}
