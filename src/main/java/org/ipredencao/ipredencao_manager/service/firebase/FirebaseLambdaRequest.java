package org.ipredencao.ipredencao_manager.service.firebase;

/**
 * Request para a Lambda Firebase Proxy
 */
public record FirebaseLambdaRequest(
    String operation,
    RequestData data
) {
    /**
     * Dados do request - campos usados conforme a operacao
     */
    public record RequestData(
        String token,       // verifyIdToken
        String email,       // createUser, getUserByEmail
        String password,    // createUser
        String displayName, // createUser, updateUser
        String uid          // getUserByUid, updateUser, deleteUser, createCustomToken
    ) {
        // Factory methods
        public static RequestData forVerifyToken(String token) {
            return new RequestData(token, null, null, null, null);
        }
        
        public static RequestData forCreateUser(String email, String password, String displayName) {
            return new RequestData(null, email, password, displayName, null);
        }
        
        public static RequestData forGetByEmail(String email) {
            return new RequestData(null, email, null, null, null);
        }
        
        public static RequestData forUid(String uid) {
            return new RequestData(null, null, null, null, uid);
        }
        
        public static RequestData forUpdateUser(String uid, String displayName) {
            return new RequestData(null, null, null, displayName, uid);
        }
    }
}
