package org.ipredencao.ipredencao_manager.service;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import jakarta.annotation.PostConstruct;
import org.ipredencao.ipredencao_manager.service.firebase.FirebaseLambdaRequest;
import org.ipredencao.ipredencao_manager.service.firebase.FirebaseLambdaRequest.RequestData;
import org.ipredencao.ipredencao_manager.service.firebase.FirebaseLambdaResponse;
import org.ipredencao.ipredencao_manager.service.firebase.FirebaseLambdaResponse.ResponseData;
import org.ipredencao.ipredencao_manager.service.firebase.FirebaseUser;
import org.ipredencao.ipredencao_manager.service.firebase.VerifiedToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class FirebaseAuthService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthService.class);

    @Value("${firebase.lambda.name:}")
    private String lambdaName;

    private AWSLambda lambdaClient;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        if (useLambda()) {
            lambdaClient = AWSLambdaClientBuilder.defaultClient();
            log.info("Firebase Lambda client inicializado: {}", lambdaName);
        }
    }

    private boolean useLambda() {
        return lambdaName != null && !lambdaName.isBlank();
    }

    public VerifiedToken verifyIdToken(String idToken) throws FirebaseAuthException {
        if (useLambda()) {
            ResponseData data = callLambda("verifyIdToken", RequestData.forVerifyToken(idToken));
            log.debug("Token verificado (lambda): {}", data.email());
            return new VerifiedToken(
                data.uid(),
                data.email(),
                data.name(),
                data.emailVerified() != null && data.emailVerified(),
                data.photoUrl()
            );
        }
        FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(idToken);
        log.debug("Token verificado (direto): {}", token.getEmail());
        return new VerifiedToken(
            token.getUid(),
            token.getEmail(),
            token.getName(),
            token.isEmailVerified(),
            token.getPicture()
        );
    }

    public FirebaseUser createUser(String email, String password, String displayName) throws FirebaseAuthException {
        if (useLambda()) {
            FirebaseUser user = toFirebaseUser(callLambda("createUser", RequestData.forCreateUser(email, password, displayName)));
            log.info("Usuario criado (lambda): {}", email);
            return user;
        }
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
            .setEmail(email)
            .setDisplayName(displayName)
            .setEmailVerified(false);
        if (password != null && !password.isBlank()) {
            request.setPassword(password);
        }
        UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
        log.info("Usuario criado (direto): {}", userRecord.getEmail());
        return toFirebaseUser(userRecord);
    }

    public FirebaseUser createUserWithoutPassword(String email, String displayName) throws FirebaseAuthException {
        return createUser(email, null, displayName);
    }

    public FirebaseUser getUserByEmail(String email) throws FirebaseAuthException {
        if (useLambda()) {
            return toFirebaseUser(callLambda("getUserByEmail", RequestData.forGetByEmail(email)));
        }
        return toFirebaseUser(FirebaseAuth.getInstance().getUserByEmail(email));
    }

    public FirebaseUser getUserByUid(String uid) throws FirebaseAuthException {
        if (useLambda()) {
            return toFirebaseUser(callLambda("getUserByUid", RequestData.forUid(uid)));
        }
        return toFirebaseUser(FirebaseAuth.getInstance().getUser(uid));
    }

    public void updateUser(String uid, String displayName) throws FirebaseAuthException {
        updateUser(uid, displayName, null, null);
    }

    public void updateUser(String uid, String displayName, String photoUrl, Boolean disabled)
            throws FirebaseAuthException {
        if (useLambda()) {
            callLambda("updateUser", RequestData.forUpdateUser(uid, displayName, photoUrl, disabled));
            log.info("Usuario atualizado (lambda): {}", uid);
            return;
        }
        UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid);
        if (displayName != null) {
            request.setDisplayName(displayName);
        }
        if (photoUrl != null) {
            request.setPhotoUrl(photoUrl);
        }
        if (disabled != null) {
            request.setDisabled(disabled);
        }
        FirebaseAuth.getInstance().updateUser(request);
        log.info("Usuario atualizado (direto): {}", uid);
    }

    public void setUserDisabled(String uid, boolean disabled) throws FirebaseAuthException {
        updateUser(uid, null, null, disabled);
    }

    public void deleteUser(String uid) throws FirebaseAuthException {
        if (useLambda()) {
            callLambda("deleteUser", RequestData.forUid(uid));
            log.info("Usuario removido (lambda): {}", uid);
            return;
        }
        FirebaseAuth.getInstance().deleteUser(uid);
        log.info("Usuario removido (direto): {}", uid);
    }

    public String createCustomToken(String uid) throws FirebaseAuthException {
        if (useLambda()) {
            ResponseData data = callLambda("createCustomToken", RequestData.forUid(uid));
            return data.token();
        }
        return FirebaseAuth.getInstance().createCustomToken(uid);
    }

    private FirebaseUser toFirebaseUser(ResponseData data) {
        return new FirebaseUser(
            data.uid(),
            data.email(),
            data.displayName(),
            Boolean.TRUE.equals(data.emailVerified()),
            data.photoUrl(),
            data.disabled()
        );
    }

    private FirebaseUser toFirebaseUser(UserRecord u) {
        return new FirebaseUser(
            u.getUid(),
            u.getEmail(),
            u.getDisplayName(),
            u.isEmailVerified(),
            u.getPhotoUrl(),
            u.isDisabled()
        );
    }

    private ResponseData callLambda(String operation, RequestData requestData) {
        try {
            FirebaseLambdaRequest request = new FirebaseLambdaRequest(operation, requestData);
            String payload = objectMapper.writeValueAsString(request);

            log.debug("Invocando Lambda {}: {}", lambdaName, operation);

            InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName(lambdaName)
                .withPayload(payload);

            InvokeResult result = lambdaClient.invoke(invokeRequest);

            String responseJson = new String(result.getPayload().array(), StandardCharsets.UTF_8);
            log.debug("Lambda response: {}", responseJson);

            FirebaseLambdaResponse response = objectMapper.readValue(responseJson, FirebaseLambdaResponse.class);

            if (response == null || !response.success()) {
                String error = response != null ? response.error() : "Empty response";
                throw new RuntimeException("Firebase Lambda error: " + error);
            }

            return response.data();

        } catch (Exception e) {
            log.error("Erro ao invocar Lambda: {}", e.getMessage());
            throw new RuntimeException("Lambda invocation failed: " + e.getMessage(), e);
        }
    }
}
