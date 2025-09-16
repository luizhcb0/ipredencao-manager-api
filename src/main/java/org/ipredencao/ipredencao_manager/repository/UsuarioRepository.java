package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.tables.records.UsuariosRecord;
import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.auth.ProviderAutenticacao;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.model.user.UsuarioQuery;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.joda.time.DateTime;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import static org.ipredencao.ipredencao_manager.jooq.tables.Usuarios.USUARIOS;

@Repository
public class UsuarioRepository {
    
    @Autowired
    private DSLContext dsl;
    
    public Usuario insert(Usuario usuario) {
        UsuariosRecord record = toRepository(usuario);
        
        UsuariosRecord saved = dsl.insertInto(USUARIOS)
                .set(record)
                .returning()
                .fetchOne();
        
        return fromRepository(saved);
    }
    
    public Usuario update(Usuario usuario) {
        UsuariosRecord record = toRepository(usuario);
        
        dsl.update(USUARIOS)
            .set(record)
            .set(USUARIOS.UPDATED_AT, DateTimeHelper.toDb(DateTime.now()))
            .where(USUARIOS.ID.eq(usuario.getId()))
            .execute();
            
        return usuario;
    }
    
    public Usuario findByFirebaseUid(String firebaseUid) {
        UsuariosRecord record = dsl.selectFrom(USUARIOS)
                .where(USUARIOS.FIREBASE_UID.eq(firebaseUid))
                .fetchOne();
        
        return fromRepository(record);
    }
    
    public Usuario findByEmail(String email) {
        UsuariosRecord record = dsl.selectFrom(USUARIOS)
                .where(USUARIOS.EMAIL.eq(email))
                .fetchOne();
        
        return fromRepository(record);
    }
    
    public List<Usuario> find(UsuarioQuery query) {
        List<Condition> conditions = buildConditions(query);
        
        Condition finalCondition = conditions.stream()
            .reduce(DSL.noCondition(), Condition::and);
        
        return dsl.selectFrom(USUARIOS)
                .where(finalCondition)
                .fetch()
                .stream()
                .map(UsuarioRepository::fromRepository)
                .toList();
    }
    
    private List<Condition> buildConditions(UsuarioQuery query) {
        List<Condition> conditions = new ArrayList<>();
        
        if (query.getId() != null) conditions.add(USUARIOS.ID.eq(query.getId()));
        if (query.getEmail() != null && !query.getEmail().trim().isEmpty()) 
            conditions.add(USUARIOS.EMAIL.eq(query.getEmail()));
        if (query.getFirebaseUid() != null && !query.getFirebaseUid().trim().isEmpty()) 
            conditions.add(USUARIOS.FIREBASE_UID.eq(query.getFirebaseUid()));
        if (query.getAccessProfile() != null) 
            conditions.add(USUARIOS.ACCESS_PROFILE.eq(
                org.ipredencao.ipredencao_manager.jooq.enums.PerfilAcesso.valueOf(query.getAccessProfile().name())
            ));
        if (query.getActive() != null) conditions.add(USUARIOS.ACTIVE.eq(query.getActive()));
        if (query.getProvider() != null)
            conditions.add(USUARIOS.PROVIDER.eq(
                org.ipredencao.ipredencao_manager.jooq.enums.ProviderAutenticacao.valueOf(query.getProvider().name())
            ));
        
        return conditions;
    }
    
    private static Usuario fromRepository(UsuariosRecord record) {
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
    
    private static UsuariosRecord toRepository(Usuario usuario) {
        UsuariosRecord record = new UsuariosRecord();
        
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
