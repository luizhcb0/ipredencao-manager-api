package org.ipredencao.ipredencao_manager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.ipredencao.ipredencao_manager.filter.JwtAuthenticationFilter;
import org.ipredencao.ipredencao_manager.model.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import jakarta.servlet.DispatcherType;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private CorsConfigurationSource corsConfigurationSource;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                
                // Endpoints públicos
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/formulario-pessoa").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/formulario-pessoa/*/foto").permitAll()
                
                // Actuator endpoints
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // Swagger/OpenAPI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Formulários (autenticados)
                .requestMatchers(HttpMethod.GET, "/api/formulario-pessoa/**").hasAnyRole("BOLETIM", "DIACONO", "PRESBITERO", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/formulario-pessoa/**").hasAnyRole("DIACONO", "PRESBITERO", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/formulario-pessoa/**").hasRole("ADMIN")
                
                // Pessoas (apenas autenticados)
                .requestMatchers(HttpMethod.GET, "/api/pessoas/**").hasAnyRole("BOLETIM", "DIACONO", "PRESBITERO", "ADMIN")
                .requestMatchers("/api/pessoas/**").hasAnyRole("DIACONO", "PRESBITERO", "ADMIN")

                // Atos oficiais (CI/IPB Cap. III) — apenas presbíteros e admins
                .requestMatchers("/api/official-acts/**").hasAnyRole("PRESBITERO", "ADMIN")

                // Serviços (serving areas): leitura BOLETIM+ (inclui os POST de
                // busca/relatório), escrita DIACONO+. Matchers de leitura antes do
                // catch-all de escrita.
                .requestMatchers(HttpMethod.POST, "/api/serving-areas/search",
                        "/api/serving-areas/members/search", "/api/serving-areas/participation-report")
                    .hasAnyRole("BOLETIM", "DIACONO", "PRESBITERO", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/serving-areas/**").hasAnyRole("BOLETIM", "DIACONO", "PRESBITERO", "ADMIN")
                .requestMatchers("/api/serving-areas/**").hasAnyRole("DIACONO", "PRESBITERO", "ADMIN")

                // Relatórios (apenas autenticados)
                .requestMatchers("/api/reports/**").hasAnyRole("BOLETIM", "DIACONO", "PRESBITERO", "ADMIN")
                
                // Dados do sistema (apenas autenticados com roles específicos)
                .requestMatchers("/api/categorias/**").hasAnyRole("BOLETIM", "DIACONO", "PRESBITERO", "ADMIN")

                .requestMatchers("/api/enderecos/**").hasAnyRole("BOLETIM", "DIACONO", "PRESBITERO", "ADMIN")

                // Usuários (admin) e perfil próprio
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/me/**").hasAnyRole("BOLETIM", "DIACONO", "PRESBITERO", "ADMIN")
                
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(),
                            ErrorResponse.of(HttpStatus.UNAUTHORIZED, "Authentication token is missing or invalid"));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(),
                            ErrorResponse.of(HttpStatus.FORBIDDEN, "You do not have permission to access this resource"));
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
