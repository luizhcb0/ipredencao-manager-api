package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.tables.records.UsuarioRecord;
import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.auth.ProviderAutenticacao;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.model.user.UsuarioQuery;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SortField;
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
    
    public List<Usuario> find(UsuarioQuery query) {
        var step = dsl.selectFrom(USUARIO).where(conditions(query));
        List<SortField<?>> order = orderBy(query);
        if (query.getPagination() != null) {
            int limit = query.getPagination().getLimit() != null ? query.getPagination().getLimit() : Integer.MAX_VALUE;
            int offset = query.getPagination().getOffset() != null ? query.getPagination().getOffset() : 0;
            return step.orderBy(order)
                .limit(limit)
                .offset(offset)
                .fetch()
                .stream()
                .map(UsuarioRepository::fromRepository)
                .toList();
        }
        return step.orderBy(order)
                .fetch()
                .stream()
                .map(UsuarioRepository::fromRepository)
                .toList();
    }

    public long count(UsuarioQuery query) {
        return dsl.fetchCount(USUARIO, conditions(query));
    }

    public void deleteById(Long id) {
        dsl.deleteFrom(USUARIO)
            .where(USUARIO.ID.eq(id))
            .execute();
    }
    
    /** Só `lastLogin` e `active`; qualquer outro valor (ou vazio) cai no nome. */
    private List<SortField<?>> orderBy(UsuarioQuery query) {
        boolean desc = query.getDir() != null && query.getDir().equalsIgnoreCase("desc");
        String sort = query.getSort();
        if ("lastLogin".equals(sort)) {
            SortField<?> lastLogin = desc ? USUARIO.LAST_LOGIN.desc() : USUARIO.LAST_LOGIN.asc();
            return List.of(lastLogin.nullsLast(), USUARIO.NAME.asc());
        }
        if ("active".equals(sort)) {
            SortField<?> active = desc ? USUARIO.ACTIVE.desc() : USUARIO.ACTIVE.asc();
            return List.of(active, USUARIO.NAME.asc());
        }
        return List.of(USUARIO.NAME.asc());
    }

    private Condition conditions(UsuarioQuery query) {
        List<Condition> conditions = new ArrayList<>();

        if (query.getId() != null) conditions.add(USUARIO.ID.eq(query.getId()));
        QueryConditions.addUnaccentedLike(conditions, USUARIO.NAME, query.getName());
        QueryConditions.addUnaccentedLike(conditions, USUARIO.EMAIL, query.getEmail());
        if (query.getFirebaseUid() != null && !query.getFirebaseUid().isBlank()) {
            conditions.add(USUARIO.FIREBASE_UID.eq(query.getFirebaseUid()));
        }
        if (query.getAccessProfile() != null) {
            conditions.add(USUARIO.ACCESS_PROFILE.eq(
                org.ipredencao.ipredencao_manager.jooq.enums.PerfilAcesso.valueOf(query.getAccessProfile().name())
            ));
        }
        if (query.getActive() != null) conditions.add(USUARIO.ACTIVE.eq(query.getActive()));
        if (query.getProvider() != null) {
            conditions.add(USUARIO.PROVIDER.eq(
                org.ipredencao.ipredencao_manager.jooq.enums.ProviderAutenticacao.valueOf(query.getProvider().name())
            ));
        }
        if (query.getPersonId() != null) conditions.add(USUARIO.PERSON_ID.eq(query.getPersonId()));

        return conditions.stream().reduce(DSL.noCondition(), Condition::and);
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
        u.setPersonId(record.getPersonId());
        
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
        record.setPersonId(usuario.getPersonId());
        
        return record;
    }
}
