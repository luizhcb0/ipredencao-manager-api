package org.ipredencao.ipredencao_manager.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FirebaseAuthService {
    
    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthService.class);
    
    public FirebaseToken verifyIdToken(String idToken) throws FirebaseAuthException {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            log.debug("Token verificado para usuário: {}", decodedToken.getEmail());
            return decodedToken;
        } catch (FirebaseAuthException e) {
            log.warn("Erro ao verificar token Firebase: {}", e);
            throw e;
        }
    }
    
    public UserRecord createUser(String email, String password, String displayName) throws FirebaseAuthException {
        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(password)
                    .setDisplayName(displayName)
                    .setEmailVerified(false);
            
            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
            log.info("Usuário criado no Firebase: {}", userRecord.getEmail());
            return userRecord;
        } catch (FirebaseAuthException e) {
            log.error("Erro ao criar usuário no Firebase", e);
            throw e;
        }
    }
    
    public UserRecord getUserByEmail(String email) throws FirebaseAuthException {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
            log.debug("Usuário encontrado no Firebase: {}", userRecord.getEmail());
            return userRecord;
        } catch (FirebaseAuthException e) {
            log.warn("Usuário não encontrado no Firebase: {}", email);
            throw e;
        }
    }
    
    public UserRecord getUserByUid(String uid) throws FirebaseAuthException {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUser(uid);
            log.debug("Usuário encontrado no Firebase por UID: {}", userRecord.getEmail());
            return userRecord;
        } catch (FirebaseAuthException e) {
            log.warn("Usuário não encontrado no Firebase por UID: {}", uid);
            throw e;
        }
    }
    
    public void updateUser(String uid, String displayName) throws FirebaseAuthException {
        try {
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(uid)
                    .setDisplayName(displayName);
            
            FirebaseAuth.getInstance().updateUser(request);
            log.info("Usuário atualizado no Firebase: {}", uid);
        } catch (FirebaseAuthException e) {
            log.error("Erro ao atualizar usuário no Firebase: {}", e.getMessage());
            throw e;
        }
    }
    
    public void deleteUser(String uid) throws FirebaseAuthException {
        try {
            FirebaseAuth.getInstance().deleteUser(uid);
            log.info("Usuário removido do Firebase: {}", uid);
        } catch (FirebaseAuthException e) {
            log.error("Erro ao remover usuário do Firebase: {}", e.getMessage());
            throw e;
        }
    }
    
    public String createCustomToken(String uid) throws FirebaseAuthException {
        try {
            String customToken = FirebaseAuth.getInstance().createCustomToken(uid);
            log.debug("Token customizado criado para usuário: {}", uid);
            return customToken;
        } catch (FirebaseAuthException e) {
            log.error("Erro ao criar token customizado: {}", e.getMessage());
            throw e;
        }
    }
}
