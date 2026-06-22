package org.ipredencao.ipredencao_manager.service.firebase;

/**
 * Usuario Firebase - substitui UserRecord que nao tem construtor vazio
 */
public record FirebaseUser(
    String uid,
    String email,
    String displayName,
    boolean emailVerified,
    String photoUrl,
    Boolean disabled
) {}
