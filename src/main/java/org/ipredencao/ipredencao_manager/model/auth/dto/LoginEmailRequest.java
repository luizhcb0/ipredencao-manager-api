package org.ipredencao.ipredencao_manager.model.auth.dto;

public class LoginEmailRequest {
    
    private String idToken;
    
    public LoginEmailRequest() {}
    
    public LoginEmailRequest(String idToken) {
        this.idToken = idToken;
    }
    
    public String getIdToken() {
        return idToken;
    }
    
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}