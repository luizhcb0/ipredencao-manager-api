package org.ipredencao.lambda;

import com.amazonaws.services.lambda.runtime.Context;
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
            ctx.getLogger().log("Processing: " + input.operation());
            return processRequest(input.operation(), input.data(), ctx);
        } catch (Exception e) {
            ctx.getLogger().log("Error: " + e.getMessage());
            return LambdaResponse.error(e.getMessage());
        }
    }
    
    private LambdaResponse processRequest(String op, LambdaRequest.RequestData data, Context ctx) {
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
            ctx.getLogger().log("Error in " + op + ": " + e.getMessage());
            return LambdaResponse.error(e.getMessage());
        }
    }
    
    private LambdaResponse verifyIdToken(String token) throws Exception {
        FirebaseToken t = FirebaseAuth.getInstance().verifyIdToken(token);
        return LambdaResponse.success(new ResponseData(
            t.getUid(), t.getEmail(), t.getName(), null, t.isEmailVerified(), null));
    }
    
    private LambdaResponse createUser(LambdaRequest.RequestData data) throws Exception {
        UserRecord user = FirebaseAuth.getInstance().createUser(
            new UserRecord.CreateRequest()
                .setEmail(data.email())
                .setPassword(data.password())
                .setDisplayName(data.displayName())
                .setEmailVerified(false));
        return LambdaResponse.success(new ResponseData(
            user.getUid(), user.getEmail(), null, user.getDisplayName(), false, null));
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
        FirebaseAuth.getInstance().updateUser(
            new UserRecord.UpdateRequest(data.uid()).setDisplayName(data.displayName()));
        return LambdaResponse.success(new ResponseData(data.uid(), null, null, null, null, null));
    }
    
    private LambdaResponse deleteUser(String uid) throws Exception {
        FirebaseAuth.getInstance().deleteUser(uid);
        return LambdaResponse.success(new ResponseData(uid, null, null, null, null, null));
    }
    
    private LambdaResponse createCustomToken(String uid) throws Exception {
        String token = FirebaseAuth.getInstance().createCustomToken(uid);
        return LambdaResponse.success(new ResponseData(null, null, null, null, null, token));
    }
    
    private ResponseData userToResponse(UserRecord u) {
        return new ResponseData(
            u.getUid(), u.getEmail(), null, u.getDisplayName(), u.isEmailVerified(), null);
    }
}
