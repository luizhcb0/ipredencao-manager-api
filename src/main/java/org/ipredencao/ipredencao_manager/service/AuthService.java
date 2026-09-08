package org.ipredencao.ipredencao_manager.service;

import com.google.firebase.auth.FirebaseAuthException;
import org.ipredencao.ipredencao_manager.model.auth.*;
import org.ipredencao.ipredencao_manager.model.auth.dto.LoginResponse;
import org.ipredencao.ipredencao_manager.model.user.SessaoUsuario;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.model.user.UsuarioQuery;
import org.ipredencao.ipredencao_manager.repository.SessaoUsuarioRepository;
import org.ipredencao.ipredencao_manager.repository.UsuarioRepository;
import org.ipredencao.ipredencao_manager.service.firebase.VerifiedToken;
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
    private UserService userService;

    public LoginResponse loginWithGoogle(String idToken, HttpServletRequest request) {
        try {
            VerifiedToken decodedToken = firebaseAuthService.verifyIdToken(idToken);

            Usuario usuario = requireExistingUsuario(
                decodedToken.uid(),
                decodedToken.email(),
                decodedToken.name(),
                ProviderAutenticacao.GOOGLE
            );

            String accessToken = jwtService.generateToken(usuario);
            String refreshToken = jwtService.generateRefreshToken(usuario);
            criarSessao(usuario, refreshToken, request);

            return new LoginResponse(accessToken, refreshToken, toUserProfile(usuario, decodedToken.picture()));

        } catch (FirebaseAuthException e) {
            log.error("Erro no login com Google: {}", e.getMessage());
            throw new IllegalArgumentException("Token Google inválido", e);
        }
    }

    public LoginResponse loginWithFacebook(String accessToken, HttpServletRequest request) {
        throw new UnsupportedOperationException("Login com Facebook ainda não implementado");
    }

    public LoginResponse loginWithApple(String idToken, String authorizationCode, String userData, HttpServletRequest request) {
        try {
            VerifiedToken decodedToken = firebaseAuthService.verifyIdToken(idToken);

            String email = decodedToken.email();
            String name = decodedToken.name();

            if ((name == null || name.isEmpty()) && userData != null && !userData.isEmpty()) {
                name = "Usuario Apple";
            }
            if (name == null || name.isEmpty()) {
                name = "Usuario Apple";
            }

            Usuario usuario = requireExistingUsuario(
                decodedToken.uid(),
                email,
                name,
                ProviderAutenticacao.APPLE
            );

            String accessToken = jwtService.generateToken(usuario);
            String refreshToken = jwtService.generateRefreshToken(usuario);
            criarSessao(usuario, refreshToken, request);

            return new LoginResponse(accessToken, refreshToken, toUserProfile(usuario, decodedToken.picture()));

        } catch (FirebaseAuthException e) {
            log.error("Erro no login com Apple: {}", e.getMessage());
            throw new IllegalArgumentException("Token Apple inválido", e);
        }
    }

    public LoginResponse loginWithEmail(String idToken, HttpServletRequest request) {
        try {
            VerifiedToken decodedToken = firebaseAuthService.verifyIdToken(idToken);

            Usuario usuario = requireExistingUsuario(
                decodedToken.uid(),
                decodedToken.email(),
                decodedToken.name(),
                ProviderAutenticacao.EMAIL
            );

            String accessToken = jwtService.generateToken(usuario);
            String refreshToken = jwtService.generateRefreshToken(usuario);
            criarSessao(usuario, refreshToken, request);

            return new LoginResponse(accessToken, refreshToken, toUserProfile(usuario, decodedToken.picture()));

        } catch (FirebaseAuthException e) {
            log.error("Token inválido: ", e);
            throw new IllegalArgumentException("Token inválido", e);
        }
    }

    public LoginResponse refreshToken(String refreshToken) {
        SessaoUsuario sessao = sessaoRepository.findByRefreshTokenHash(
            jwtService.hashToken(refreshToken)
        );

        if (sessao == null || !sessao.getAtivo() || sessao.isExpirada()) {
            throw new IllegalArgumentException("Refresh token inválido");
        }

        Usuario usuario = findOne(UsuarioQuery.builder().id(sessao.getUsuarioId()).build());
        if (usuario == null) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }

        if (!Boolean.TRUE.equals(usuario.getActive())) {
            throw new IllegalArgumentException("Usuário inativo");
        }

        String newAccessToken = jwtService.generateToken(usuario);

        sessao.atualizarUltimoUso();
        sessaoRepository.update(sessao);

        return new LoginResponse(newAccessToken, refreshToken, toUserProfile(usuario, null));
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            SessaoUsuario sessao = sessaoRepository.findByRefreshTokenHash(
                jwtService.hashToken(refreshToken)
            );

            if (sessao != null) {
                sessao.setAtivo(false);
                sessaoRepository.update(sessao);
            }
        }
    }

    private Usuario requireExistingUsuario(String firebaseUid, String email,
                                           String name, ProviderAutenticacao provider) {
        Usuario usuario = findOne(UsuarioQuery.builder().firebaseUid(firebaseUid).build());

        if (usuario == null && email != null) {
            usuario = findOne(UsuarioQuery.builder().email(email).build());
            if (usuario != null && usuario.getFirebaseUid() == null) {
                usuario.setFirebaseUid(firebaseUid);
                usuarioRepository.update(usuario);
            }
        }

        if (usuario == null) {
            throw new IllegalArgumentException("Conta não encontrada. Solicite convite ao administrador.");
        }

        if (!Boolean.TRUE.equals(usuario.getActive())) {
            throw new IllegalArgumentException("Conta inativa. Entre em contato com o administrador.");
        }

        usuario.setLastLogin(DateTime.now());
        usuario.resetarTentativasFalhou();
        if (name != null && !name.isBlank() && (usuario.getName() == null || usuario.getName().isBlank())) {
            usuario.setName(name);
        }
        if (usuario.getProvider() == null) {
            usuario.setProvider(provider);
        }
        usuarioRepository.update(usuario);
        log.debug("Usuário autenticado: {}", email);

        return usuario;
    }

    private SessaoUsuario criarSessao(Usuario usuario, String refreshToken, HttpServletRequest request) {
        SessaoUsuario sessao = new SessaoUsuario();
        sessao.setUsuarioId(usuario.getId());
        sessao.setRefreshTokenHash(jwtService.hashToken(refreshToken));
        sessao.setDataCriacao(DateTime.now());
        sessao.setDataExpiracao(DateTime.now().plusDays(30));
        sessao.setDataUltimoUso(DateTime.now());
        sessao.setUserAgent(request.getHeader("User-Agent"));
        sessao.setAtivo(true);

        return sessaoRepository.insert(sessao);
    }

    private UserProfile toUserProfile(Usuario usuario, String photoUrl) {
        return userService.toUserProfile(usuario, photoUrl);
    }

    private Usuario findOne(UsuarioQuery query) {
        return usuarioRepository.find(query).stream().findFirst().orElse(null);
    }
}
