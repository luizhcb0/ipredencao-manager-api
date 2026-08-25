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

    static final String UNAUTHORIZED_MESSAGE = "Sessão expirada ou inválida. Faça login novamente.";
    static final String FORBIDDEN_MESSAGE = "Você não tem permissão para acessar este recurso.";

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
                // O liveness e o alvo do health check do App Runner, que chama sem
                // credencial: sem esta regra ele cai no /actuator/** abaixo, responde
                // 401 e a instancia e substituida em loop. Expoe apenas "ping".
                .requestMatchers("/actuator/health", "/actuator/health/liveness").permitAll()
                .requestMatchers("/actuator/**").hasAnyRole(Roles.adminNames())
                
                // Swagger/OpenAPI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Formulários: DIACONO+. A captação pública fica acima.
                .requestMatchers(HttpMethod.POST, "/api/formulario-pessoa/search",
                        "/api/formulario-pessoa/processar").hasAnyRole(Roles.staffNames())
                .requestMatchers(HttpMethod.GET, "/api/formulario-pessoa/**").hasAnyRole(Roles.staffNames())
                .requestMatchers(HttpMethod.PUT, "/api/formulario-pessoa/**").hasAnyRole(Roles.staffNames())
                .requestMatchers(HttpMethod.DELETE, "/api/formulario-pessoa/**").hasAnyRole(Roles.adminNames())
                
                // Pessoas: leitura BOLETIM+, escrita DIACONO+. A busca é POST e precisa da
                // própria linha — o matcher decide antes do @PreAuthorize do controller.
                .requestMatchers(HttpMethod.POST, "/api/pessoas/search").hasAnyRole(Roles.anyNames())
                .requestMatchers(HttpMethod.GET, "/api/pessoas/*/history", "/api/pessoas/*/notes")
                    .hasAnyRole(Roles.staffNames())
                .requestMatchers(HttpMethod.GET, "/api/pessoas/**").hasAnyRole(Roles.anyNames())
                .requestMatchers("/api/pessoas/**").hasAnyRole(Roles.staffNames())

                // Atos oficiais (CI/IPB Cap. III) — apenas presbíteros e admins
                .requestMatchers("/api/official-acts/**").hasAnyRole(Roles.elderNames())

                // Serviços (serving areas): leitura BOLETIM+ (inclui os POST de
                // busca/relatório), escrita DIACONO+. Matchers de leitura antes do
                // catch-all de escrita.
                .requestMatchers(HttpMethod.POST, "/api/serving-areas/search",
                        "/api/serving-areas/members/search", "/api/serving-areas/participation-report")
                    .hasAnyRole(Roles.anyNames())
                .requestMatchers(HttpMethod.GET, "/api/serving-areas/**").hasAnyRole(Roles.anyNames())
                .requestMatchers("/api/serving-areas/**").hasAnyRole(Roles.staffNames())

                // Relatórios (apenas autenticados)
                .requestMatchers("/api/reports/**").hasAnyRole(Roles.anyNames())
                
                // Dados do sistema (apenas autenticados com roles específicos)
                .requestMatchers("/api/categorias/**").hasAnyRole(Roles.anyNames())

                .requestMatchers("/api/enderecos/**").hasAnyRole(Roles.anyNames())

                // Usuários (admin) e perfil próprio
                .requestMatchers("/api/users/**").hasAnyRole(Roles.adminNames())
                .requestMatchers("/api/me/**").hasAnyRole(Roles.anyNames())
                
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(),
                            ErrorResponse.of(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(),
                            ErrorResponse.of(HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE));
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
