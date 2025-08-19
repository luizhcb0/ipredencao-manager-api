package org.ipredencao.ipredencao_manager.model.auth;

public class LogoutRequest {
    
    private String refreshToken;
    
    public LogoutRequest() {}
    
    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}