package org.ipredencao.ipredencao_manager.model.auth.dto;

public class LoginFacebookRequest {
    
    private String accessToken;
    
    public LoginFacebookRequest() {}
    
    public LoginFacebookRequest(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
