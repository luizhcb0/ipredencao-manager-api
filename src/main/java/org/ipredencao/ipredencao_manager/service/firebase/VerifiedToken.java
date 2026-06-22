package org.ipredencao.ipredencao_manager.service.firebase;

/**
 * Token Firebase verificado - substitui FirebaseToken que é final
 */
public record VerifiedToken(
    String uid,
    String email,
    String name,
    boolean emailVerified,
    String picture
) {}
