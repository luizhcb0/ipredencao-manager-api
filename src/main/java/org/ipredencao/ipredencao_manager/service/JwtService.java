package org.ipredencao.ipredencao_manager.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.ipredencao.ipredencao_manager.config.JwtConfig;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    
    @Autowired
    private JwtConfig jwtConfig;
    
    public String extrairEmail(String token) {
        return extrairClaim(token, Claims::getSubject);
    }
    
    public String extrairRole(String token) {
        return extrairClaim(token, claims -> claims.get("role", String.class));
    }
    
    public Long extrairUserId(String token) {
        return extrairClaim(token, claims -> claims.get("userId", Long.class));
    }
    
    public <T> T extrairClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extrairTodasClaims(token);
        return claimsResolver.apply(claims);
    }
    
    public String gerarToken(Usuario usuario) {
        return gerarToken(new HashMap<>(), usuario);
    }
    
    public String gerarToken(Map<String, Object> extraClaims, Usuario usuario) {
        extraClaims.put("role", usuario.getAccessProfile().name());
        extraClaims.put("userId", usuario.getId());
        extraClaims.put("name", usuario.getName());
        
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(usuario.getEmail())
                .setIssuer(jwtConfig.getIssuer())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration() * 1000))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public String gerarRefreshToken(Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        claims.put("userId", usuario.getId());
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(usuario.getEmail())
                .setIssuer(jwtConfig.getIssuer())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshExpiration() * 1000))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    public boolean isTokenValido(String token) {
        try {
            return !isTokenExpirado(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean isTokenExpirado(String token) {
        return extrairExpiracao(token).before(new Date());
    }
    
    private Date extrairExpiracao(String token) {
        return extrairClaim(token, Claims::getExpiration);
    }
    
    private Claims extrairTodasClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    private SecretKey getSignInKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash do token", e);
        }
    }
}
