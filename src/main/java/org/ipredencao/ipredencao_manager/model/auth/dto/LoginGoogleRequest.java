package org.ipredencao.ipredencao_manager.model.auth.dto;

public class LoginGoogleRequest {
    
    private String idToken;
    
    public LoginGoogleRequest() {}
    
    public LoginGoogleRequest(String idToken) {
        this.idToken = idToken;
    }
    
    public String getIdToken() {
        return idToken;
    }
    
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
