package org.ipredencao.ipredencao_manager.service.firebase;

/**
 * Request para a Lambda Firebase Proxy
 */
public record FirebaseLambdaRequest(
    String operation,
    RequestData data
) {
    public record RequestData(
        String token,
        String email,
        String password,
        String displayName,
        String uid,
        String photoUrl,
        Boolean disabled
    ) {
        public static RequestData forVerifyToken(String token) {
            return new RequestData(token, null, null, null, null, null, null);
        }

        public static RequestData forCreateUser(String email, String password, String displayName) {
            return new RequestData(null, email, password, displayName, null, null, null);
        }

        public static RequestData forGetByEmail(String email) {
            return new RequestData(null, email, null, null, null, null, null);
        }

        public static RequestData forUid(String uid) {
            return new RequestData(null, null, null, null, uid, null, null);
        }

        public static RequestData forUpdateUser(String uid, String displayName, String photoUrl, Boolean disabled) {
            return new RequestData(null, null, null, displayName, uid, photoUrl, disabled);
        }
    }
}
