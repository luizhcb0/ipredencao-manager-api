package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.tables.records.AuditoriaUsuarioRecord;
import org.ipredencao.ipredencao_manager.model.AcaoAuditoria;
import org.ipredencao.ipredencao_manager.model.AuditoriaUsuario;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.joda.time.DateTime;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.ipredencao.ipredencao_manager.jooq.tables.AuditoriaUsuario.AUDITORIA_USUARIO;

@Repository
public class AuditoriaUsuarioRepository {
    
    @Autowired
    private DSLContext dsl;
    
    public AuditoriaUsuario insert(AuditoriaUsuario auditoria) {
        AuditoriaUsuarioRecord record = toRepository(auditoria);
        
        AuditoriaUsuarioRecord saved = dsl.insertInto(AUDITORIA_USUARIO)
                .set(record)
                .returning()
                .fetchOne();
        
        return fromRepository(saved);
    }
    
    public List<AuditoriaUsuario> findByUsuarioId(Long usuarioId) {
        return dsl.selectFrom(AUDITORIA_USUARIO)
                .where(AUDITORIA_USUARIO.USUARIO_ID.eq(usuarioId))
                .orderBy(AUDITORIA_USUARIO.DATA_ACAO.desc())
                .fetch()
                .stream()
                .map(AuditoriaUsuarioRepository::fromRepository)
                .toList();
    }
    
    public List<AuditoriaUsuario> findByAcao(AcaoAuditoria acao) {
        return dsl.selectFrom(AUDITORIA_USUARIO)
                .where(AUDITORIA_USUARIO.ACAO.eq(
                    org.ipredencao.ipredencao_manager.jooq.enums.AcaoAuditoria.valueOf(acao.name())
                ))
                .orderBy(AUDITORIA_USUARIO.DATA_ACAO.desc())
                .fetch()
                .stream()
                .map(AuditoriaUsuarioRepository::fromRepository)
                .toList();
    }
    
    public List<AuditoriaUsuario> findAcoesAnonimas() {
        return dsl.selectFrom(AUDITORIA_USUARIO)
                .where(AUDITORIA_USUARIO.IS_ANONYMOUS.eq(true))
                .orderBy(AUDITORIA_USUARIO.DATA_ACAO.desc())
                .fetch()
                .stream()
                .map(AuditoriaUsuarioRepository::fromRepository)
                .toList();
    }
    
    public List<AuditoriaUsuario> findRecent(int limit) {
        return dsl.selectFrom(AUDITORIA_USUARIO)
                .orderBy(AUDITORIA_USUARIO.DATA_ACAO.desc())
                .limit(limit)
                .fetch()
                .stream()
                .map(AuditoriaUsuarioRepository::fromRepository)
                .toList();
    }
    
    private static AuditoriaUsuario fromRepository(AuditoriaUsuarioRecord record) {
        if (record == null) return null;
        
        AuditoriaUsuario a = new AuditoriaUsuario();
        a.setId(record.getId());
        a.setUsuarioId(record.getUsuarioId());
        
        if (record.getAcao() != null)
            a.setAcao(AcaoAuditoria.valueOf(record.getAcao().name()));
        
        a.setRecurso(record.getRecurso());
        a.setRecursoId(record.getRecursoId());
        
        // Converter JSONB para Map (implementação simplificada)
        if (record.getDetalhes() != null) {
            // Por enquanto, deixamos vazio - seria necessário implementar conversão JSONB -> Map
            // a.setDetalhes(convertJsonbToMap(record.getDetalhes()));
        }
        
        a.setIpAddress(record.getIpAddress() != null ? record.getIpAddress().toString() : null);
        a.setUserAgent(record.getUserAgent());
        a.setIsAnonymous(record.getIsAnonymous());
        a.setDataAcao(DateTimeHelper.fromDb(record.getDataAcao()));
        
        return a;
    }
    
    private static AuditoriaUsuarioRecord toRepository(AuditoriaUsuario auditoria) {
        AuditoriaUsuarioRecord record = new AuditoriaUsuarioRecord();
        
        record.setUsuarioId(auditoria.getUsuarioId());
        
        if (auditoria.getAcao() != null)
            record.setAcao(
                org.ipredencao.ipredencao_manager.jooq.enums.AcaoAuditoria.valueOf(auditoria.getAcao().name())
            );
        
        record.setRecurso(auditoria.getRecurso());
        record.setRecursoId(auditoria.getRecursoId());
        
        // Converter Map para JSONB (implementação simplificada)
        if (auditoria.getDetalhes() != null && !auditoria.getDetalhes().isEmpty()) {
            // Por enquanto, deixamos null - seria necessário implementar conversão Map -> JSONB
            // record.setDetalhes(convertMapToJsonb(auditoria.getDetalhes()));
        }
        
        record.setIpAddress(auditoria.getIpAddress());
        record.setUserAgent(auditoria.getUserAgent());
        record.setIsAnonymous(auditoria.getIsAnonymous());
        record.setDataAcao(DateTimeHelper.toDb(auditoria.getDataAcao()));
        
        return record;
    }
}
