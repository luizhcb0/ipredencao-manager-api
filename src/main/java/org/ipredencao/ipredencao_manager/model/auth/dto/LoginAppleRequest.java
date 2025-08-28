package org.ipredencao.ipredencao_manager.model.auth.dto;

public class LoginAppleRequest {
    
    private String idToken;
    private String authorizationCode;
    private String user; // JSON string com dados do usuário (apenas no primeiro login)
    
    public LoginAppleRequest() {}
    
    public LoginAppleRequest(String idToken, String authorizationCode) {
        this.idToken = idToken;
        this.authorizationCode = authorizationCode;
    }
    
    public LoginAppleRequest(String idToken, String authorizationCode, String user) {
        this.idToken = idToken;
        this.authorizationCode = authorizationCode;
        this.user = user;
    }
    
    public String getIdToken() {
        return idToken;
    }
    
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
    
    public String getAuthorizationCode() {
        return authorizationCode;
    }
    
    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }
    
    public String getUser() {
        return user;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
}
