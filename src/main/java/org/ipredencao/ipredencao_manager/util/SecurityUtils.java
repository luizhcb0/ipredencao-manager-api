package org.ipredencao.ipredencao_manager.util;

import org.ipredencao.ipredencao_manager.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityUtils {
    
    @Autowired
    private JwtService jwtService;
    
    /**
     * Extrai o ID do usuário atual do token JWT
     * @return ID do usuário ou null se não autenticado
     */
    public Long getCurrentUserId() {
        try {
            // Obter o request atual
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                return jwtService.extrairUserId(jwt);
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extrai o email do usuário atual do SecurityContext
     * @return Email do usuário ou null se não autenticado
     */
    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
    
    /**
     * Verifica se existe um usuário autenticado
     * @return true se autenticado, false caso contrário
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getName());
    }
    
    /**
     * Extrai a role do usuário atual
     * @return Role do usuário ou null se não autenticado
     */
    public String getCurrentUserRole() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String jwt = authHeader.substring(7);
                return jwtService.extrairRole(jwt);
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
