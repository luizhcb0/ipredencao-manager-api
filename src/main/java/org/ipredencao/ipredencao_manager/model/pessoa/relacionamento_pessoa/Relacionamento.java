package org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa;

import org.ipredencao.ipredencao_manager.model.pessoa.TipoRelacionamento;
import org.joda.time.DateTime;

public class Relacionamento {
    private Long pessoaId;
    private Long pessoaRelacionadaId;
    private String nomePessoaRelacionada;
    private TipoRelacionamento tipoRelacionamento;
    private DateTime inicioRelacionamento;

    public Long getPessoaId() { return pessoaId; }
    public void setPessoaId(Long pessoaId) { this.pessoaId = pessoaId; }
    public Long getPessoaRelacionadaId() { return pessoaRelacionadaId; }
    public void setPessoaRelacionadaId(Long pessoaRelacionadaId) { this.pessoaRelacionadaId = pessoaRelacionadaId; }
    public String getNomePessoaRelacionada() { return nomePessoaRelacionada; }
    public void setNomePessoaRelacionada(String nomePessoaRelacionada) { this.nomePessoaRelacionada = nomePessoaRelacionada; }
    public TipoRelacionamento getTipoRelacionamento() { return tipoRelacionamento; }
    public void setTipoRelacionamento(TipoRelacionamento tipoRelacionamento) { this.tipoRelacionamento = tipoRelacionamento; }
    public DateTime getInicioRelacionamento() { return inicioRelacionamento; }
    public void setInicioRelacionamento(DateTime inicioRelacionamento) { this.inicioRelacionamento = inicioRelacionamento; }
}