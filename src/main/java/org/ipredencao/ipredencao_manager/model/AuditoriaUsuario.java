package org.ipredencao.ipredencao_manager.model;

import org.joda.time.DateTime;
import java.util.HashMap;
import java.util.Map;

public class AuditoriaUsuario {
    
    private Long id;
    
    private Long usuarioId; // NULL para ações anônimas
    
    private AcaoAuditoria acao;
    
    private String recurso;
    
    private Long recursoId;
    
    private Map<String, Object> detalhes = new HashMap<>();
    
    private String ipAddress;
    
    private String userAgent;
    
    private Boolean isAnonymous = false;
    
    private DateTime dataAcao;
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUsuarioId() {
        return usuarioId;
    }
    
    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
    
    public AcaoAuditoria getAcao() {
        return acao;
    }
    
    public void setAcao(AcaoAuditoria acao) {
        this.acao = acao;
    }
    
    public String getRecurso() {
        return recurso;
    }
    
    public void setRecurso(String recurso) {
        this.recurso = recurso;
    }
    
    public Long getRecursoId() {
        return recursoId;
    }
    
    public void setRecursoId(Long recursoId) {
        this.recursoId = recursoId;
    }
    
    public Map<String, Object> getDetalhes() {
        return detalhes;
    }
    
    public void setDetalhes(Map<String, Object> detalhes) {
        this.detalhes = detalhes;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public Boolean getIsAnonymous() {
        return isAnonymous;
    }
    
    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }
    
    public DateTime getDataAcao() {
        return dataAcao;
    }
    
    public void setDataAcao(DateTime dataAcao) {
        this.dataAcao = dataAcao;
    }
}
