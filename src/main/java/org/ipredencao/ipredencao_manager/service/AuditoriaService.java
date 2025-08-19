package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.AcaoAuditoria;
import org.ipredencao.ipredencao_manager.model.AuditoriaUsuario;
import org.ipredencao.ipredencao_manager.repository.AuditoriaUsuarioRepository;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditoriaService {
    
    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    
    @Autowired
    private AuditoriaUsuarioRepository auditoriaRepository;
    
    public void registrarAcao(Long usuarioId, AcaoAuditoria acao, String recurso, 
                             Long recursoId, String ipAddress, String userAgent) {
        registrarAcao(usuarioId, acao, recurso, recursoId, ipAddress, userAgent, new HashMap<>());
    }
    
    public void registrarAcao(Long usuarioId, AcaoAuditoria acao, String recurso, 
                             Long recursoId, String ipAddress, String userAgent, 
                             Map<String, Object> detalhes) {
        try {
            AuditoriaUsuario auditoria = new AuditoriaUsuario();
            auditoria.setUsuarioId(usuarioId);
            auditoria.setAcao(acao);
            auditoria.setRecurso(recurso);
            auditoria.setRecursoId(recursoId);
            auditoria.setDetalhes(detalhes);
            auditoria.setIpAddress(ipAddress);
            auditoria.setUserAgent(userAgent);
            auditoria.setIsAnonymous(false);
            auditoria.setDataAcao(DateTime.now());
            
            auditoriaRepository.insert(auditoria);
            
            log.debug("Auditoria registrada: {} - {} - Usuário: {}", acao, recurso, usuarioId);
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria: {}", e.getMessage(), e);
            // Não propagar a exceção para não afetar o fluxo principal
        }
    }
    
    public void registrarAcaoAnonima(AcaoAuditoria acao, String recurso, Long recursoId, 
                                   String ipAddress, String userAgent) {
        registrarAcaoAnonima(acao, recurso, recursoId, ipAddress, userAgent, new HashMap<>());
    }
    
    public void registrarAcaoAnonima(AcaoAuditoria acao, String recurso, Long recursoId, 
                                   String ipAddress, String userAgent, Map<String, Object> detalhes) {
        try {
            AuditoriaUsuario auditoria = new AuditoriaUsuario();
            auditoria.setUsuarioId(null); // NULL = anônimo
            auditoria.setAcao(acao);
            auditoria.setRecurso(recurso);
            auditoria.setRecursoId(recursoId);
            auditoria.setDetalhes(detalhes);
            auditoria.setIpAddress(ipAddress);
            auditoria.setUserAgent(userAgent);
            auditoria.setIsAnonymous(true);
            auditoria.setDataAcao(DateTime.now());
            
            auditoriaRepository.insert(auditoria);
            
            log.debug("Auditoria anônima registrada: {} - {} - IP: {}", acao, recurso, ipAddress);
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria anônima: {}", e.getMessage(), e);
            // Não propagar a exceção para não afetar o fluxo principal
        }
    }
    
    public List<AuditoriaUsuario> buscarPorUsuario(Long usuarioId) {
        return auditoriaRepository.findByUsuarioId(usuarioId);
    }
    
    public List<AuditoriaUsuario> buscarPorAcao(AcaoAuditoria acao) {
        return auditoriaRepository.findByAcao(acao);
    }
    
    public List<AuditoriaUsuario> buscarAcoesAnonimas() {
        return auditoriaRepository.findAcoesAnonimas();
    }
    
    public List<AuditoriaUsuario> buscarRecentes(int limit) {
        return auditoriaRepository.findRecent(limit);
    }
    
    public void registrarLogin(Long usuarioId, String ipAddress, String userAgent) {
        Map<String, Object> detalhes = new HashMap<>();
        detalhes.put("timestamp", DateTime.now().toString());
        
        registrarAcao(usuarioId, AcaoAuditoria.LOGIN, "USUARIO", usuarioId, 
                     ipAddress, userAgent, detalhes);
    }
    
    public void registrarLogout(Long usuarioId, String ipAddress, String userAgent) {
        Map<String, Object> detalhes = new HashMap<>();
        detalhes.put("timestamp", DateTime.now().toString());
        
        registrarAcao(usuarioId, AcaoAuditoria.LOGOUT, "USUARIO", usuarioId, 
                     ipAddress, userAgent, detalhes);
    }
    
    public void registrarCriacaoFormularioAnonimo(Long formularioId, String ipAddress, String userAgent) {
        Map<String, Object> detalhes = new HashMap<>();
        detalhes.put("timestamp", DateTime.now().toString());
        detalhes.put("tipo", "formulario_anonimo");
        
        registrarAcaoAnonima(AcaoAuditoria.CREATE_FORMULARIO, "FORMULARIO", formularioId, 
                           ipAddress, userAgent, detalhes);
    }
}
