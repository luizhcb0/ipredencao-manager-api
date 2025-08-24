package org.ipredencao.ipredencao_manager.model;

import org.joda.time.DateTime;

public class SessaoUsuario {
    
    private Long id;
    
    private Long usuarioId;
    
    private String refreshTokenHash;
    
    private DateTime dataCriacao;
    
    private DateTime dataExpiracao;
    
    private DateTime dataUltimoUso;
    
    private String userAgent;
    
    private String dispositivo;
    
    private String localizacao;
    
    private Boolean ativo = true;
    
    // Métodos de conveniência
    public boolean isExpirada() {
        return dataExpiracao.isBeforeNow();
    }
    
    public void atualizarUltimoUso() {
        this.dataUltimoUso = DateTime.now();
    }
    
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
    
    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }
    
    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }
    
    public DateTime getDataCriacao() {
        return dataCriacao;
    }
    
    public void setDataCriacao(DateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
    
    public DateTime getDataExpiracao() {
        return dataExpiracao;
    }
    
    public void setDataExpiracao(DateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }
    
    public DateTime getDataUltimoUso() {
        return dataUltimoUso;
    }
    
    public void setDataUltimoUso(DateTime dataUltimoUso) {
        this.dataUltimoUso = dataUltimoUso;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getDispositivo() {
        return dispositivo;
    }
    
    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }
    
    public String getLocalizacao() {
        return localizacao;
    }
    
    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }
    
    public Boolean getAtivo() {
        return ativo;
    }
    
    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}
