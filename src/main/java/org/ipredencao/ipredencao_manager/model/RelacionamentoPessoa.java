package org.ipredencao.ipredencao_manager.model;

import org.joda.time.DateTime;


public class RelacionamentoPessoa {
    private Long id;
    private Pessoa pessoaRelacionada;
    private TipoRelacionamento tipoRelacionamento;
    private DateTime inicioRelacionamento;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pessoa getPessoaRelacionada() { return pessoaRelacionada; }
    public void setPessoaRelacionada(Pessoa pessoaRelacionada) { this.pessoaRelacionada = pessoaRelacionada; }
    public TipoRelacionamento getTipoRelacionamento() { return tipoRelacionamento; }
    public void setTipoRelacionamento(TipoRelacionamento tipoRelacionamento) { this.tipoRelacionamento = tipoRelacionamento; }
    public DateTime getInicioRelacionamento() { return inicioRelacionamento; }
    public void setInicioRelacionamento(DateTime inicioRelacionamento) { this.inicioRelacionamento = inicioRelacionamento; }
}
