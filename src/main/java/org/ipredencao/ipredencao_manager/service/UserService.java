package org.ipredencao.ipredencao_manager.service;

import com.google.firebase.auth.FirebaseAuthException;
import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.auth.ProviderAutenticacao;
import org.ipredencao.ipredencao_manager.model.auth.UserProfile;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.model.user.UsuarioQuery;
import org.ipredencao.ipredencao_manager.model.user.dto.CreateUserRequest;
import org.ipredencao.ipredencao_manager.model.user.dto.UpdateProfileRequest;
import org.ipredencao.ipredencao_manager.model.user.dto.UpdateUserRequest;
import org.ipredencao.ipredencao_manager.model.user.dto.UserSummaryResponse;
import org.ipredencao.ipredencao_manager.repository.SessaoUsuarioRepository;
import org.ipredencao.ipredencao_manager.repository.UsuarioRepository;
import org.ipredencao.ipredencao_manager.service.firebase.FirebaseUser;
import org.joda.time.DateTime;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** Mensagem 409 quando FK updated_by impede exclusão (V008). */
    public static final String CANNOT_DELETE_USER_WITH_REFERENCES =
        "Não é possível excluir este usuário porque existem registros vinculados a ele. Desative-o em vez de excluir.";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SessaoUsuarioRepository sessaoRepository;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    public List<UserSummaryResponse> listUsers(Boolean active, PerfilAcesso profile, String search) {
        UsuarioQuery.Builder builder = UsuarioQuery.builder();
        if (active != null) {
            builder.active(active);
        }
        if (profile != null) {
            builder.accessProfile(profile);
        }

        return usuarioRepository.find(builder.build()).stream()
            .filter(u -> matchesSearch(u, search))
            .sorted(Comparator.comparing(Usuario::getName, String.CASE_INSENSITIVE_ORDER))
            .map(UserSummaryResponse::from)
            .toList();
    }

    public UserSummaryResponse getUser(Long id) {
        Usuario usuario = requireUsuario(id);
        return UserSummaryResponse.from(usuario);
    }

    public UserSummaryResponse createUser(CreateUserRequest request) {
        if (request.getProfile() == null) {
            throw new IllegalArgumentException("Tipo de acesso é obrigatório");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email é obrigatório");
        }

        String email = request.getEmail().trim();
        String name = request.getName().trim();

        Usuario existing = usuarioRepository.findByEmail(email);
        if (existing != null) {
            throw new IllegalStateException("Usuário já existe com este email");
        }

        FirebaseUser firebaseUser;
        boolean createdInFirebase = true;
        try {
            firebaseUser = firebaseAuthService.createUserWithoutPassword(email, name);
        } catch (FirebaseAuthException | RuntimeException e) {
            if (!FirebaseAuthService.isEmailAlreadyExists(e)) {
                throw firebaseInviteFailure(e);
            }
            log.info("Convite vinculado a conta Firebase já existente: {}", email);
            firebaseUser = fetchFirebaseUserByEmail(email);
            createdInFirebase = false;
        }

        Usuario usuario = new Usuario();
        usuario.setFirebaseUid(firebaseUser.uid());
        usuario.setEmail(email);
        usuario.setName(name);
        usuario.setProvider(ProviderAutenticacao.EMAIL);
        usuario.setAddedAt(DateTime.now());
        usuario.setActive(true);
        usuario.setAccessProfile(request.getProfile());

        if (!createdInFirebase) {
            syncFirebaseDisplayName(usuario);
        }

        try {
            return UserSummaryResponse.from(usuarioRepository.insert(usuario));
        } catch (Exception e) {
            if (createdInFirebase) {
                try {
                    firebaseAuthService.deleteUser(firebaseUser.uid());
                } catch (FirebaseAuthException ex) {
                    log.error("Falha ao compensar usuário Firebase {}: {}", firebaseUser.uid(), ex.getMessage());
                }
            }
            throw e;
        }
    }

    private FirebaseUser fetchFirebaseUserByEmail(String email) {
        try {
            return firebaseAuthService.getUserByEmail(email);
        } catch (FirebaseAuthException e) {
            throw firebaseInviteFailure(e);
        }
    }

    private IllegalArgumentException firebaseInviteFailure(Throwable e) {
        log.error("Erro ao criar usuário no Firebase: {}", e.getMessage());
        return new IllegalArgumentException("Erro ao criar usuário: " + e.getMessage(), e);
    }

    public UserSummaryResponse updateUser(Long id, UpdateUserRequest request, Long currentUserId) {
        Usuario usuario = requireUsuario(id);
        validateAdminMutation(usuario, request, currentUserId);

        if (request.getName() != null && !request.getName().isBlank()) {
            usuario.setName(request.getName().trim());
            syncFirebaseDisplayName(usuario);
        }

        if (request.getAccessProfile() != null && request.getAccessProfile() != usuario.getAccessProfile()) {
            ensureNotLastActiveAdmin(usuario, request.getAccessProfile(), request.getActive());
            usuario.setAccessProfile(request.getAccessProfile());
        }

        if (request.getActive() != null && !request.getActive().equals(usuario.getActive())) {
            if (!request.getActive()) {
                ensureNotLastActiveAdmin(usuario, usuario.getAccessProfile(), false);
            }
            usuario.setActive(request.getActive());
            syncFirebaseDisabled(usuario);
            if (!request.getActive()) {
                sessaoRepository.invalidarSessoesUsuario(usuario.getId());
            }
        }

        usuarioRepository.update(usuario);
        return UserSummaryResponse.from(usuario);
    }

    @Transactional
    public void deleteUser(Long id, Long currentUserId) {
        Usuario usuario = requireUsuario(id);

        if (usuario.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("Você não pode excluir sua própria conta");
        }

        if (Boolean.TRUE.equals(usuario.getActive()) && usuario.getAccessProfile() == PerfilAcesso.ADMIN) {
            ensureNotLastActiveAdmin(usuario, usuario.getAccessProfile(), false);
        }

        try {
            usuarioRepository.deleteById(id);
        } catch (DataIntegrityViolationException | IntegrityConstraintViolationException e) {
            throw new IllegalStateException(CANNOT_DELETE_USER_WITH_REFERENCES, e);
        }

        try {
            firebaseAuthService.deleteUser(usuario.getFirebaseUid());
        } catch (FirebaseAuthException e) {
            log.error("Erro ao excluir usuário no Firebase: {}", e.getMessage());
            throw new IllegalArgumentException("Erro ao excluir usuário no Firebase: " + e.getMessage(), e);
        }

        log.info("Usuário excluído: {} ({})", usuario.getEmail(), id);
    }

    public UserProfile getProfile(Long userId) {
        Usuario usuario = requireUsuario(userId);
        return toUserProfile(usuario, fetchPhotoUrl(usuario));
    }

    public UserProfile updateProfile(Long userId, UpdateProfileRequest request) {
        Usuario usuario = requireUsuario(userId);
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }

        usuario.setName(request.getName().trim());
        syncFirebaseDisplayName(usuario);
        usuarioRepository.update(usuario);

        return toUserProfile(usuario, fetchPhotoUrl(usuario));
    }

    public UserProfile toUserProfile(Usuario usuario, String photoUrl) {
        UserProfile profile = new UserProfile();
        profile.setId(usuario.getId());
        profile.setEmail(usuario.getEmail());
        profile.setNome(usuario.getName());
        profile.setFotoUrl(photoUrl);
        profile.setPerfilAcesso(usuario.getAccessProfile().name());
        profile.setDataUltimoLogin(usuario.getLastLogin() != null ? usuario.getLastLogin().toString() : null);
        return profile;
    }

    private Usuario requireUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id);
        if (usuario == null) {
            throw new NoSuchElementException("Usuário não encontrado");
        }
        return usuario;
    }

    private void validateAdminMutation(Usuario target, UpdateUserRequest request, Long currentUserId) {
        if (!target.getId().equals(currentUserId)) {
            return;
        }

        if (request.getActive() != null && !request.getActive()) {
            throw new IllegalArgumentException("Você não pode desativar sua própria conta");
        }

        if (request.getAccessProfile() != null
                && request.getAccessProfile() != PerfilAcesso.ADMIN
                && target.getAccessProfile() == PerfilAcesso.ADMIN) {
            throw new IllegalArgumentException("Você não pode remover seu próprio perfil de administrador");
        }
    }

    private void ensureNotLastActiveAdmin(Usuario target, PerfilAcesso newProfile, Boolean newActive) {
        if (target.getAccessProfile() != PerfilAcesso.ADMIN || !Boolean.TRUE.equals(target.getActive())) {
            return;
        }

        boolean demotingAdmin = newProfile != null && newProfile != PerfilAcesso.ADMIN;
        boolean deactivating = newActive != null && !newActive;

        if (demotingAdmin || deactivating) {
            long activeAdmins = usuarioRepository.countActiveByProfile(PerfilAcesso.ADMIN);
            if (activeAdmins <= 1) {
                throw new IllegalArgumentException("Não é possível remover o último administrador ativo");
            }
        }
    }

    private void syncFirebaseDisplayName(Usuario usuario) {
        try {
            firebaseAuthService.updateUser(usuario.getFirebaseUid(), usuario.getName());
        } catch (FirebaseAuthException e) {
            log.error("Erro ao atualizar displayName no Firebase: {}", e.getMessage());
            throw new IllegalArgumentException("Erro ao atualizar usuário no Firebase: " + e.getMessage(), e);
        }
    }

    private void syncFirebaseDisabled(Usuario usuario) {
        try {
            firebaseAuthService.setUserDisabled(usuario.getFirebaseUid(), !Boolean.TRUE.equals(usuario.getActive()));
        } catch (FirebaseAuthException e) {
            log.error("Erro ao atualizar status no Firebase: {}", e.getMessage());
            throw new IllegalArgumentException("Erro ao atualizar usuário no Firebase: " + e.getMessage(), e);
        }
    }

    private String fetchPhotoUrl(Usuario usuario) {
        try {
            FirebaseUser firebaseUser = firebaseAuthService.getUserByUid(usuario.getFirebaseUid());
            return firebaseUser.photoUrl();
        } catch (FirebaseAuthException e) {
            log.warn("Não foi possível obter foto do Firebase para {}: {}", usuario.getEmail(), e.getMessage());
            return null;
        }
    }

    private boolean matchesSearch(Usuario usuario, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String term = search.trim().toLowerCase();
        return usuario.getName().toLowerCase().contains(term)
            || usuario.getEmail().toLowerCase().contains(term);
    }
}
