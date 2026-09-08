package org.ipredencao.ipredencao_manager.filter;

import org.ipredencao.ipredencao_manager.model.auth.AuthUser;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.model.user.UsuarioQuery;
import org.ipredencao.ipredencao_manager.repository.UsuarioRepository;
import org.ipredencao.ipredencao_manager.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        jwt = authHeader.substring(7);
        
        try {
            userEmail = jwtService.extractEmail(jwt);
            
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                if (jwtService.isTokenValido(jwt)) {
                    // VALIDAÇÃO DE SEGURANÇA: Verificar se o usuário existe e está ativo
                    Long userId = jwtService.extractUserId(jwt);
                    Usuario usuario = usuarioRepository.find(UsuarioQuery.builder().id(userId).build())
                        .stream().findFirst().orElse(null);
                    
                    if (usuario == null) {
                        log.warn("Token válido mas usuário não existe: userId={}", userId);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Token inválido: usuário não existe\"}");
                        return;
                    }
                    
                    if (!usuario.getActive()) {
                        log.warn("Token válido mas usuário está inativo: userId={}, email={}", userId, userEmail);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Token inválido: usuário inativo\"}");
                        return;
                    }
                    
                    String tokenRole = jwtService.extractRole(jwt);
                    String actualRole = usuario.getAccessProfile().name();
                    
                    // VALIDAÇÃO DE SEGURANÇA: Verificar se a role no token ainda é válida
                    if (!tokenRole.equals(actualRole)) {
                        log.warn("Role no token não corresponde à role atual do usuário. Token: {}, Atual: {}, userId={}", 
                                 tokenRole, actualRole, userId);
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Token inválido: permissões alteradas\"}");
                        return;
                    }
                    
                    List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + actualRole)
                    );

                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            new AuthUser(usuario.getId(), usuario.getEmail(), usuario.getAccessProfile()),
                            null,
                            authorities);
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("Usuário autenticado: {} com role: {}", userEmail, actualRole);
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao processar token JWT: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
}
