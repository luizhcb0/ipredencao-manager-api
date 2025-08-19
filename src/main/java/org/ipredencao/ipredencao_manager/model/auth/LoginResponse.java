package org.ipredencao.ipredencao_manager.model.auth;

public class LoginResponse {
    
    private String accessToken;
    private String refreshToken;
    private UserProfile user;
    private long expiresIn;
    
    public LoginResponse() {}
    
    public LoginResponse(String accessToken, String refreshToken, UserProfile user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.expiresIn = 3600; // 1 hora em segundos
    }
    
    public LoginResponse(String accessToken, String refreshToken, UserProfile user, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
        this.expiresIn = expiresIn;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public UserProfile getUser() {
        return user;
    }
    
    public void setUser(UserProfile user) {
        this.user = user;
    }
    
    public long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}