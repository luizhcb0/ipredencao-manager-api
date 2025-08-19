package org.ipredencao.ipredencao_manager.service;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.ipredencao.ipredencao_manager.model.*;
import org.ipredencao.ipredencao_manager.model.auth.*;
import org.ipredencao.ipredencao_manager.repository.SessaoUsuarioRepository;
import org.ipredencao.ipredencao_manager.repository.UsuarioRepository;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {
    
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private SessaoUsuarioRepository sessaoRepository;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private FirebaseAuthService firebaseAuthService;
    
    @Autowired
    private AuditoriaService auditoriaService;
    
    public LoginResponse loginComGoogle(String idToken, HttpServletRequest request) {
        try {
            // Verificar token com Firebase
            FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(idToken);
            
            // Criar/atualizar usuário
            Usuario usuario = criarOuAtualizarUsuario(
                decodedToken.getUid(),
                decodedToken.getEmail(),
                decodedToken.getName(),
                ProviderAutenticacao.GOOGLE
            );
            
            // Gerar tokens
            String accessToken = jwtService.gerarToken(usuario);
            String refreshToken = jwtService.gerarRefreshToken(usuario);
            
            // Criar sessão
            SessaoUsuario sessao = criarSessao(usuario, refreshToken, request);
            
            // Auditoria
            auditoriaService.registrarLogin(
                usuario.getId(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
            );
            
            return new LoginResponse(accessToken, refreshToken, toUserProfile(usuario));
            
        } catch (FirebaseAuthException e) {
            log.error("Erro no login com Google: {}", e.getMessage());
            throw new IllegalArgumentException("Token Google inválido", e);
        }
    }
    
    public LoginResponse loginComFacebook(String accessToken, HttpServletRequest request) {
        // TODO: Implementar verificação com Facebook Graph API
        throw new UnsupportedOperationException("Login com Facebook ainda não implementado");
    }
    
    public LoginResponse loginComApple(String idToken, String authorizationCode, String userData, HttpServletRequest request) {
        try {
            // Verificar token com Firebase (Apple Sign-In)
            FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(idToken);
            
            // Extrair dados do usuário (Apple pode não fornecer nome em logins subsequentes)
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();
            
            // Se não tem nome no token e foi fornecido userData, extrair do JSON
            if ((name == null || name.isEmpty()) && userData != null && !userData.isEmpty()) {
                try {
                    // Implementação simplificada - em produção usar Jackson ObjectMapper
                    if (userData.contains("\"firstName\"")) {
                        // Extrair nome do JSON userData fornecido pelo Apple
                        name = "Usuario Apple"; // Fallback simples
                    }
                } catch (Exception e) {
                    log.warn("Erro ao extrair dados do usuário Apple: {}", e.getMessage());
                    name = "Usuario Apple";
                }
            }
            
            if (name == null || name.isEmpty()) {
                name = "Usuario Apple";
            }
            
            // Criar/atualizar usuário
            Usuario usuario = criarOuAtualizarUsuario(
                decodedToken.getUid(),
                email,
                name,
                ProviderAutenticacao.APPLE
            );
            
            // Gerar tokens
            String accessToken = jwtService.gerarToken(usuario);
            String refreshToken = jwtService.gerarRefreshToken(usuario);
            
            // Criar sessão
            SessaoUsuario sessao = criarSessao(usuario, refreshToken, request);
            
            // Auditoria
            auditoriaService.registrarLogin(
                usuario.getId(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
            );
            
            return new LoginResponse(accessToken, refreshToken, toUserProfile(usuario));
            
        } catch (FirebaseAuthException e) {
            log.error("Erro no login com Apple: {}", e.getMessage());
            throw new IllegalArgumentException("Token Apple inválido", e);
        }
    }
    
    public LoginResponse loginComEmail(String email, String senha, HttpServletRequest request) {
        try {
            // Buscar usuário existente
            Usuario usuario = usuarioRepository.findByEmail(email);
            if (usuario == null) {
                throw new IllegalArgumentException("Usuário não encontrado");
            }
            
            // Verificar se conta não está bloqueada
            if (usuario.isContaBloqueada()) {
                throw new IllegalArgumentException("Conta bloqueada temporariamente");
            }
            
            // Verificar credenciais com Firebase
            FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(senha); // Simplificado
            
            // Atualizar último login
            usuario.setLastLogin(DateTime.now());
            usuario.resetarTentativasFalhou();
            usuarioRepository.update(usuario);
            
            // Gerar tokens
            String accessToken = jwtService.gerarToken(usuario);
            String refreshToken = jwtService.gerarRefreshToken(usuario);
            
            // Criar sessão
            SessaoUsuario sessao = criarSessao(usuario, refreshToken, request);
            
            // Auditoria
            auditoriaService.registrarLogin(
                usuario.getId(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
            );
            
            return new LoginResponse(accessToken, refreshToken, toUserProfile(usuario));
            
        } catch (FirebaseAuthException e) {
            // Registrar tentativa de login falhada
            Usuario usuario = usuarioRepository.findByEmail(email);
            if (usuario != null) {
                usuario.incrementarTentativasFalhou();
                usuarioRepository.update(usuario);
            }
            
            log.warn("Tentativa de login falhada para: {}", email);
            throw new IllegalArgumentException("Credenciais inválidas", e);
        }
    }
    
    public LoginResponse register(RegisterRequest registerRequest, HttpServletRequest request) {
        try {
            // Verificar se usuário já existe
            Usuario usuarioExistente = usuarioRepository.findByEmail(registerRequest.getEmail());
            if (usuarioExistente != null) {
                throw new IllegalArgumentException("Usuário já existe com este email");
            }
            
            // Criar usuário no Firebase
            var userRecord = firebaseAuthService.createUser(
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getName()
            );
            
            // Criar usuário no banco
            Usuario usuario = new Usuario();
            usuario.setFirebaseUid(userRecord.getUid());
            usuario.setEmail(registerRequest.getEmail());
            usuario.setName(registerRequest.getName());
            usuario.setProvider(ProviderAutenticacao.EMAIL);
            usuario.setAddedAt(DateTime.now());
            usuario.setActive(true);
            usuario.setAccessProfile(PerfilAcesso.BOLETIM); // Perfil padrão
            
            usuario = usuarioRepository.insert(usuario);
            
            // Gerar tokens
            String accessToken = jwtService.gerarToken(usuario);
            String refreshToken = jwtService.gerarRefreshToken(usuario);
            
            // Criar sessão
            SessaoUsuario sessao = criarSessao(usuario, refreshToken, request);
            
            // Auditoria
            auditoriaService.registrarAcao(
                usuario.getId(),
                AcaoAuditoria.REGISTER,
                "USUARIO",
                usuario.getId(),
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
            );
            
            return new LoginResponse(accessToken, refreshToken, toUserProfile(usuario));
            
        } catch (FirebaseAuthException e) {
            log.error("Erro ao registrar usuário: {}", e.getMessage());
            throw new IllegalArgumentException("Erro ao criar usuário: " + e.getMessage(), e);
        }
    }
    
    public LoginResponse refreshToken(String refreshToken) {
        // Buscar sessão
        SessaoUsuario sessao = sessaoRepository.findByRefreshTokenHash(
            jwtService.hashToken(refreshToken)
        );
        
        if (sessao == null || !sessao.getAtivo() || sessao.isExpirada()) {
            throw new IllegalArgumentException("Refresh token inválido");
        }
        
        // Buscar usuário
        Usuario usuario = usuarioRepository.find(
            UsuarioQuery.builder().id(sessao.getUsuarioId()).build()
        ).stream().findFirst().orElseThrow(() -> 
            new IllegalArgumentException("Usuário não encontrado")
        );
        
        // Gerar novo access token
        String newAccessToken = jwtService.gerarToken(usuario);
        
        // Atualizar último uso da sessão
        sessao.atualizarUltimoUso();
        sessaoRepository.update(sessao);
        
        return new LoginResponse(newAccessToken, refreshToken, toUserProfile(usuario));
    }
    
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            SessaoUsuario sessao = sessaoRepository.findByRefreshTokenHash(
                jwtService.hashToken(refreshToken)
            );
            
            if (sessao != null) {
                sessao.setAtivo(false);
                sessaoRepository.update(sessao);
                
                // Auditoria
                auditoriaService.registrarLogout(
                    sessao.getUsuarioId(),
                    null,
                    null
                );
            }
        }
    }
    
    private Usuario criarOuAtualizarUsuario(String firebaseUid, String email, 
                                          String name, ProviderAutenticacao provider) {
        Usuario usuario = usuarioRepository.findByFirebaseUid(firebaseUid);
        
        if (usuario == null) {
            // Criar novo usuário
            usuario = new Usuario();
            usuario.setFirebaseUid(firebaseUid);
            usuario.setEmail(email);
            usuario.setName(name);
            usuario.setProvider(provider);
            usuario.setAddedAt(DateTime.now());
            usuario.setActive(true);
            usuario.setAccessProfile(PerfilAcesso.BOLETIM); // Perfil padrão
            
            usuario = usuarioRepository.insert(usuario);
            log.info("Novo usuário criado: {}", email);
        } else {
            // Atualizar dados do usuário
            usuario.setLastLogin(DateTime.now());
            usuario.resetarTentativasFalhou();
            usuarioRepository.update(usuario);
            log.debug("Usuário atualizado: {}", email);
        }
        
        return usuario;
    }
    
    private SessaoUsuario criarSessao(Usuario usuario, String refreshToken, HttpServletRequest request) {
        SessaoUsuario sessao = new SessaoUsuario();
        sessao.setUsuarioId(usuario.getId());
        sessao.setRefreshTokenHash(jwtService.hashToken(refreshToken));
        sessao.setDataCriacao(DateTime.now());
        sessao.setDataExpiracao(DateTime.now().plusDays(30)); // 30 dias
        sessao.setDataUltimoUso(DateTime.now());
        sessao.setIpAddress(request.getRemoteAddr());
        sessao.setUserAgent(request.getHeader("User-Agent"));
        sessao.setAtivo(true);
        
        return sessaoRepository.insert(sessao);
    }
    
    private UserProfile toUserProfile(Usuario usuario) {
        UserProfile profile = new UserProfile();
        profile.setId(usuario.getId());
        profile.setEmail(usuario.getEmail());
        profile.setNome(usuario.getName());
        profile.setPerfilAcesso(usuario.getAccessProfile().name());
        profile.setProvider(usuario.getProvider().name());
        profile.setDataUltimoLogin(usuario.getLastLogin() != null ? 
            usuario.getLastLogin().toString() : null);
        return profile;
    }
}
