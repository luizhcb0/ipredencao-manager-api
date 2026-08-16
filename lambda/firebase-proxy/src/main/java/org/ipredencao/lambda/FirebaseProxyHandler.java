package org.ipredencao.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.LambdaRuntime;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.ipredencao.lambda.model.LambdaRequest;
import org.ipredencao.lambda.model.LambdaResponse;
import org.ipredencao.lambda.model.LambdaResponse.ResponseData;

import java.io.ByteArrayInputStream;

/**
 * Handler para invocacao direta via AWS SDK (sem API Gateway)
 */
public class FirebaseProxyHandler implements RequestHandler<LambdaRequest, LambdaResponse> {

    private static final LambdaLogger log = LambdaRuntime.getLogger();

    private static boolean initialized = false;

    public FirebaseProxyHandler() {
        initializeFirebase();
    }

    private synchronized void initializeFirebase() {
        if (initialized) return;
        try {
            String json = System.getenv("FIREBASE_SERVICE_ACCOUNT_KEY_CONTENT");
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(json.getBytes()));

            // O token so seria buscado na primeira invocacao, dentro do handler.
            // Buscando aqui, na fase de init, o SnapStart captura o resultado no
            // snapshot. Falha nao e fatal: o SDK refaz sob demanda.
            try {
                credentials.refreshIfExpired();
            } catch (Exception e) {
                log.log("Priming de credenciais falhou, seguindo sem: " + e.getMessage());
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials).build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            initialized = true;
        } catch (Exception e) {
            throw new RuntimeException("Firebase init failed", e);
        }
    }

    @Override
    public LambdaResponse handleRequest(LambdaRequest input, Context ctx) {
        try {
            log.log("Processing: " + input.operation());
            return processRequest(input.operation(), input.data());
        } catch (Exception e) {
            log.log("Error: " + e.getMessage());
            return LambdaResponse.error(e.getMessage());
        }
    }

    private LambdaResponse processRequest(String op, LambdaRequest.RequestData data) {
        try {
            return switch (op) {
                case "verifyIdToken" -> verifyIdToken(data.token());
                case "createUser" -> createUser(data);
                case "getUserByEmail" -> getUserByEmail(data.email());
                case "getUserByUid" -> getUserByUid(data.uid());
                case "updateUser" -> updateUser(data);
                case "deleteUser" -> deleteUser(data.uid());
                case "createCustomToken" -> createCustomToken(data.uid());
                default -> LambdaResponse.error("Unknown operation: " + op);
            };
        } catch (Exception e) {
            log.log("Error in " + op + ": " + e.getMessage());
            return LambdaResponse.error(e.getMessage());
        }
    }

    private LambdaResponse verifyIdToken(String token) throws Exception {
        FirebaseToken t = FirebaseAuth.getInstance().verifyIdToken(token);
        return LambdaResponse.success(new ResponseData(
            t.getUid(), t.getEmail(), t.getName(), null, t.isEmailVerified(), null, t.getPicture(), null));
    }

    private LambdaResponse createUser(LambdaRequest.RequestData data) throws Exception {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
            .setEmail(data.email())
            .setDisplayName(data.displayName())
            .setEmailVerified(false);
        if (data.password() != null && !data.password().isBlank()) {
            request.setPassword(data.password());
        }
        UserRecord user = FirebaseAuth.getInstance().createUser(request);
        return LambdaResponse.success(userToResponse(user));
    }

    private LambdaResponse getUserByEmail(String email) throws Exception {
        UserRecord u = FirebaseAuth.getInstance().getUserByEmail(email);
        return LambdaResponse.success(userToResponse(u));
    }

    private LambdaResponse getUserByUid(String uid) throws Exception {
        UserRecord u = FirebaseAuth.getInstance().getUser(uid);
        return LambdaResponse.success(userToResponse(u));
    }

    private LambdaResponse updateUser(LambdaRequest.RequestData data) throws Exception {
        UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(data.uid());
        if (data.displayName() != null) {
            request.setDisplayName(data.displayName());
        }
        if (data.photoUrl() != null) {
            request.setPhotoUrl(data.photoUrl());
        }
        if (data.disabled() != null) {
            request.setDisabled(data.disabled());
        }
        FirebaseAuth.getInstance().updateUser(request);
        return LambdaResponse.success(new ResponseData(data.uid(), null, null, null, null, null, null, null));
    }

    private LambdaResponse deleteUser(String uid) throws Exception {
        FirebaseAuth.getInstance().deleteUser(uid);
        return LambdaResponse.success(new ResponseData(uid, null, null, null, null, null, null, null));
    }

    private LambdaResponse createCustomToken(String uid) throws Exception {
        String token = FirebaseAuth.getInstance().createCustomToken(uid);
        return LambdaResponse.success(new ResponseData(null, null, null, null, null, token, null, null));
    }

    private ResponseData userToResponse(UserRecord u) {
        return new ResponseData(
            u.getUid(), u.getEmail(), null, u.getDisplayName(), u.isEmailVerified(), null, u.getPhotoUrl(), u.isDisabled());
    }
}
