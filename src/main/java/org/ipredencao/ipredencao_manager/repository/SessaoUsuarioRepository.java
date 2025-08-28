package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.tables.records.SessoesUsuarioRecord;
import org.ipredencao.ipredencao_manager.model.user.SessaoUsuario;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.ipredencao.ipredencao_manager.jooq.tables.SessoesUsuario.SESSOES_USUARIO;

@Repository
public class SessaoUsuarioRepository {
    
    @Autowired
    private DSLContext dsl;
    
    public SessaoUsuario insert(SessaoUsuario sessao) {
        SessoesUsuarioRecord record = toRepository(sessao);
        
        SessoesUsuarioRecord saved = dsl.insertInto(SESSOES_USUARIO)
                .set(record)
                .returning()
                .fetchOne();
        
        return fromRepository(saved);
    }
    
    public SessaoUsuario update(SessaoUsuario sessao) {
        SessoesUsuarioRecord record = toRepository(sessao);
        
        dsl.update(SESSOES_USUARIO)
            .set(record)
            .where(SESSOES_USUARIO.ID.eq(sessao.getId()))
            .execute();
            
        return sessao;
    }
    
    public SessaoUsuario findByRefreshTokenHash(String refreshTokenHash) {
        SessoesUsuarioRecord record = dsl.selectFrom(SESSOES_USUARIO)
                .where(SESSOES_USUARIO.REFRESH_TOKEN_HASH.eq(refreshTokenHash))
                .and(SESSOES_USUARIO.ATIVO.eq(true))
                .fetchOne();
        
        return fromRepository(record);
    }
    
    public List<SessaoUsuario> findByUsuarioId(Long usuarioId) {
        return dsl.selectFrom(SESSOES_USUARIO)
                .where(SESSOES_USUARIO.USUARIO_ID.eq(usuarioId))
                .and(SESSOES_USUARIO.ATIVO.eq(true))
                .fetch()
                .stream()
                .map(SessaoUsuarioRepository::fromRepository)
                .toList();
    }
    
    public void invalidarSessoesExpiradas() {
        dsl.update(SESSOES_USUARIO)
            .set(SESSOES_USUARIO.ATIVO, false)
            .where(SESSOES_USUARIO.DATA_EXPIRACAO.lessThan(DateTimeHelper.toDb(DateTime.now())))
            .and(SESSOES_USUARIO.ATIVO.eq(true))
            .execute();
    }
    
    public void invalidarSessoesUsuario(Long usuarioId) {
        dsl.update(SESSOES_USUARIO)
            .set(SESSOES_USUARIO.ATIVO, false)
            .where(SESSOES_USUARIO.USUARIO_ID.eq(usuarioId))
            .and(SESSOES_USUARIO.ATIVO.eq(true))
            .execute();
    }
    
    private static SessaoUsuario fromRepository(SessoesUsuarioRecord record) {
        if (record == null) return null;
        
        SessaoUsuario s = new SessaoUsuario();
        s.setId(record.getId());
        s.setUsuarioId(record.getUsuarioId());
        s.setRefreshTokenHash(record.getRefreshTokenHash());
        s.setDataCriacao(DateTimeHelper.fromDb(record.getDataCriacao()));
        s.setDataExpiracao(DateTimeHelper.fromDb(record.getDataExpiracao()));
        s.setDataUltimoUso(DateTimeHelper.fromDb(record.getDataUltimoUso()));
        s.setUserAgent(record.getUserAgent());
        s.setDispositivo(record.getDispositivo());
        s.setLocalizacao(record.getLocalizacao());
        s.setAtivo(record.getAtivo());
        
        return s;
    }
    
    private static SessoesUsuarioRecord toRepository(SessaoUsuario sessao) {
        SessoesUsuarioRecord record = new SessoesUsuarioRecord();
        
        record.setUsuarioId(sessao.getUsuarioId());
        record.setRefreshTokenHash(sessao.getRefreshTokenHash());
        record.setDataCriacao(DateTimeHelper.toDb(sessao.getDataCriacao()));
        record.setDataExpiracao(DateTimeHelper.toDb(sessao.getDataExpiracao()));
        record.setDataUltimoUso(DateTimeHelper.toDb(sessao.getDataUltimoUso()));
        record.setUserAgent(sessao.getUserAgent());
        record.setDispositivo(sessao.getDispositivo());
        record.setLocalizacao(sessao.getLocalizacao());
        record.setAtivo(sessao.getAtivo());
        
        return record;
    }
}
