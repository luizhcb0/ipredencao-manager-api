package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.tables.records.UsuarioRecord;
import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.auth.ProviderAutenticacao;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.model.user.UsuarioQuery;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;

import static org.ipredencao.ipredencao_manager.jooq.tables.Usuario.USUARIO;

@Repository
public class UsuarioRepository {
    
    @Autowired
    private DSLContext dsl;
    
    public Usuario insert(Usuario usuario) {
        UsuarioRecord record = toRepository(usuario);
        
        UsuarioRecord saved = dsl.insertInto(USUARIO)
                .set(record)
                .returning()
                .fetchOne();
        
        return fromRepository(saved);
    }
    
    public Usuario update(Usuario usuario) {
        UsuarioRecord record = toRepository(usuario);
        
        dsl.update(USUARIO)
            .set(record)
            .where(USUARIO.ID.eq(usuario.getId()))
            .execute();
            
        return usuario;
    }
    
    public Usuario findByFirebaseUid(String firebaseUid) {
        UsuarioRecord record = dsl.selectFrom(USUARIO)
                .where(USUARIO.FIREBASE_UID.eq(firebaseUid))
                .fetchOne();
        
        return fromRepository(record);
    }
    
    public Usuario findById(Long id) {
        UsuarioRecord record = dsl.selectFrom(USUARIO)
                .where(USUARIO.ID.eq(id))
                .fetchOne();
        
        return fromRepository(record);
    }
    
    public Usuario findByEmail(String email) {
        UsuarioRecord record = dsl.selectFrom(USUARIO)
                .where(USUARIO.EMAIL.eq(email))
                .fetchOne();
        
        return fromRepository(record);
    }
    
    public List<Usuario> find(UsuarioQuery query) {
        List<Condition> conditions = buildConditions(query);

        Condition finalCondition = conditions.stream()
            .reduce(DSL.noCondition(), Condition::and);

        return dsl.selectFrom(USUARIO)
                .where(finalCondition)
                .orderBy(USUARIO.NAME.asc())
                .fetch()
                .stream()
                .map(UsuarioRepository::fromRepository)
                .toList();
    }

    public long countActiveByProfile(PerfilAcesso profile) {
        return dsl.fetchCount(
            USUARIO,
            USUARIO.ACCESS_PROFILE.eq(
                org.ipredencao.ipredencao_manager.jooq.enums.PerfilAcesso.valueOf(profile.name())
            ).and(USUARIO.ACTIVE.eq(true))
        );
    }

    public void deleteById(Long id) {
        dsl.deleteFrom(USUARIO)
            .where(USUARIO.ID.eq(id))
            .execute();
    }
    
    private List<Condition> buildConditions(UsuarioQuery query) {
        List<Condition> conditions = new ArrayList<>();
        
        if (query.getId() != null) conditions.add(USUARIO.ID.eq(query.getId()));
        if (query.getEmail() != null && !query.getEmail().trim().isEmpty()) 
            conditions.add(USUARIO.EMAIL.eq(query.getEmail()));
        if (query.getFirebaseUid() != null && !query.getFirebaseUid().trim().isEmpty()) 
            conditions.add(USUARIO.FIREBASE_UID.eq(query.getFirebaseUid()));
        if (query.getAccessProfile() != null) 
            conditions.add(USUARIO.ACCESS_PROFILE.eq(
                org.ipredencao.ipredencao_manager.jooq.enums.PerfilAcesso.valueOf(query.getAccessProfile().name())
            ));
        if (query.getActive() != null) conditions.add(USUARIO.ACTIVE.eq(query.getActive()));
        if (query.getProvider() != null)
            conditions.add(USUARIO.PROVIDER.eq(
                org.ipredencao.ipredencao_manager.jooq.enums.ProviderAutenticacao.valueOf(query.getProvider().name())
            ));
        
        return conditions;
    }
    
    private static Usuario fromRepository(UsuarioRecord record) {
        if (record == null) return null;
        
        Usuario u = new Usuario();
        u.setId(record.getId());
        u.setFirebaseUid(record.getFirebaseUid());
        u.setEmail(record.getEmail());
        u.setName(record.getName());
        
        if (record.getAccessProfile() != null)
            u.setAccessProfile(PerfilAcesso.valueOf(record.getAccessProfile().name()));
        
        u.setAddedAt(DateTimeHelper.fromDb(record.getAddedAt()));
        u.setLastLogin(DateTimeHelper.fromDb(record.getLastLogin()));
        u.setUpdatedAt(DateTimeHelper.fromDb(record.getUpdatedAt()));
        u.setActive(record.getActive());
        
        if (record.getProvider() != null)
            u.setProvider(ProviderAutenticacao.valueOf(record.getProvider().name()));
            
        u.setFailedLoginAttempts(record.getFailedLoginAttempts());
        u.setBlockedUntil(DateTimeHelper.fromDb(record.getBlockedUntil()));
        
        return u;
    }
    
    private static UsuarioRecord toRepository(Usuario usuario) {
        UsuarioRecord record = new UsuarioRecord();
        
        record.setFirebaseUid(usuario.getFirebaseUid());
        record.setEmail(usuario.getEmail());
        record.setName(usuario.getName());
        
        if (usuario.getAccessProfile() != null)
            record.setAccessProfile(
                org.ipredencao.ipredencao_manager.jooq.enums.PerfilAcesso.valueOf(usuario.getAccessProfile().name())
            );
            
        record.setAddedAt(DateTimeHelper.toDb(usuario.getAddedAt()));
        record.setLastLogin(DateTimeHelper.toDb(usuario.getLastLogin()));
        record.setUpdatedAt(DateTimeHelper.toDb(usuario.getUpdatedAt()));
        record.setActive(usuario.getActive());
        
        if (usuario.getProvider() != null)
            record.setProvider(
                org.ipredencao.ipredencao_manager.jooq.enums.ProviderAutenticacao.valueOf(usuario.getProvider().name())
            );
            
        record.setFailedLoginAttempts(usuario.getFailedLoginAttempts());
        record.setBlockedUntil(DateTimeHelper.toDb(usuario.getBlockedUntil()));
        
        return record;
    }
}
