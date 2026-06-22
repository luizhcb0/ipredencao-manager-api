package org.ipredencao.ipredencao_manager.service.firebase;

/**
 * Response da Lambda Firebase Proxy
 */
public record FirebaseLambdaResponse(
    boolean success,
    ResponseData data,
    String error
) {
    public record ResponseData(
        String uid,
        String email,
        String name,
        String displayName,
        Boolean emailVerified,
        String token,
        String photoUrl,
        Boolean disabled
    ) {}
}
