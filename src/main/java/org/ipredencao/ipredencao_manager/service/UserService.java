package org.ipredencao.ipredencao_manager.service;

import com.google.firebase.auth.FirebaseAuthException;
import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.auth.ProviderAutenticacao;
import org.ipredencao.ipredencao_manager.model.auth.UserProfile;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
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
    public static final String PERSON_ALREADY_LINKED =
        "Esta pessoa já está vinculada a outro usuário";
    public static final String PERSON_NOT_FOUND = "Pessoa não encontrada";
    public static final String USER_PROVISION_WARNING =
        "A pessoa foi processada, mas não foi possível criar o usuário.";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SessaoUsuarioRepository sessaoRepository;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @Autowired
    private PessoaService pessoaService;

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
        if (request.profile() == null) {
            throw new IllegalArgumentException("Tipo de acesso é obrigatório");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email é obrigatório");
        }

        String email = request.email().trim();
        String name = request.name().trim();

        if (usuarioRepository.findByEmail(email) != null) {
            throw new IllegalStateException("Usuário já existe com este email");
        }

        FirebaseIdentity identity = resolveFirebaseIdentity(email, name);

        Usuario usuario = newEmailUser(identity.user().uid(), email, name, request.profile(), true);
        applyPersonLink(usuario, request.personId());

        if (!identity.created()) {
            syncFirebaseDisplayName(usuario);
        }

        return UserSummaryResponse.from(insertCompensating(usuario, identity));
    }

    /** `created` distingue identidade nova (compensável) de conta Firebase reaproveitada. */
    private record FirebaseIdentity(FirebaseUser user, boolean created) {}

    private FirebaseIdentity resolveFirebaseIdentity(String email, String name) {
        try {
            return new FirebaseIdentity(firebaseAuthService.createUserWithoutPassword(email, name), true);
        } catch (FirebaseAuthException | RuntimeException e) {
            if (!FirebaseAuthService.isEmailAlreadyExists(e)) {
                throw firebaseInviteFailure(e);
            }
            log.info("Vinculado a conta Firebase já existente: {}", email);
            return new FirebaseIdentity(fetchFirebaseUserByEmail(email), false);
        }
    }

    private Usuario newEmailUser(String uid, String email, String name, PerfilAcesso profile, boolean active) {
        Usuario usuario = new Usuario();
        usuario.setFirebaseUid(uid);
        usuario.setEmail(email);
        usuario.setName(name);
        usuario.setProvider(ProviderAutenticacao.EMAIL);
        usuario.setAddedAt(DateTime.now());
        usuario.setActive(active);
        usuario.setAccessProfile(profile);
        return usuario;
    }

    private Usuario insertCompensating(Usuario usuario, FirebaseIdentity identity) {
        try {
            return usuarioRepository.insert(usuario);
        } catch (Exception e) {
            if (identity.created()) {
                compensateFirebaseCreate(identity.user().uid());
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

        if (request.isPersonIdPresent()) {
            applyPersonLink(usuario, request.getPersonId());
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

        ensureNotLastActiveAdmin(usuario, usuario.getAccessProfile(), false);

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

    /**
     * Cria ou vincula um usuário inativo para pessoa elegível (agregadores 2, 3 ou 5) com e-mail.
     * Idempotente: vínculo já existente ou usuário do mesmo e-mail livre é só associado.
     */
    @Transactional
    public void ensureInactiveUserForPerson(Pessoa person) {
        PerfilAcesso profile = profileForEligiblePerson(person);
        if (profile == null) return;

        String email = normalizeEmail(person.getEmail());
        if (email == null) return;

        if (usuarioRepository.findByPersonId(person.getId()) != null) return;

        Usuario byEmail = usuarioRepository.findByNormalizedEmail(email);
        if (byEmail != null) {
            if (byEmail.getPersonId() != null && !byEmail.getPersonId().equals(person.getId())) {
                throw new IllegalStateException(PERSON_ALREADY_LINKED);
            }
            byEmail.setPersonId(person.getId());
            usuarioRepository.update(byEmail);
            return;
        }

        FirebaseIdentity identity = resolveFirebaseIdentity(email, person.getNome());

        try {
            firebaseAuthService.setUserDisabled(identity.user().uid(), true);
        } catch (FirebaseAuthException e) {
            if (identity.created()) {
                compensateFirebaseCreate(identity.user().uid());
            }
            throw firebaseInviteFailure(e);
        }

        Usuario usuario = newEmailUser(identity.user().uid(), email, person.getNome(), profile, false);
        usuario.setPersonId(person.getId());
        insertCompensating(usuario, identity);
    }

    private static PerfilAcesso profileForEligiblePerson(Pessoa person) {
        if (person == null || person.getCategoria() == null) return null;
        Long aggregatorId = person.getCategoria().getAgregadorCategoriaId();
        if (Long.valueOf(2L).equals(aggregatorId) || Long.valueOf(3L).equals(aggregatorId)) {
            return PerfilAcesso.MEMBER;
        }
        if (Long.valueOf(5L).equals(aggregatorId)) {
            return PerfilAcesso.MEMBERSHIP_CANDIDATE;
        }
        return null;
    }

    private void applyPersonLink(Usuario usuario, Long personId) {
        if (personId == null) {
            usuario.setPersonId(null);
            return;
        }
        try {
            pessoaService.findById(personId);
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException(PERSON_NOT_FOUND);
        }
        Usuario existing = usuarioRepository.findByPersonId(personId);
        if (existing != null && !existing.getId().equals(usuario.getId())) {
            throw new IllegalStateException(PERSON_ALREADY_LINKED);
        }
        usuario.setPersonId(personId);
    }

    private void compensateFirebaseCreate(String uid) {
        try {
            firebaseAuthService.deleteUser(uid);
        } catch (FirebaseAuthException ex) {
            log.error("Falha ao compensar usuário Firebase {}: {}", uid, ex.getMessage());
        }
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) return null;
        return email.trim().toLowerCase();
    }
}
