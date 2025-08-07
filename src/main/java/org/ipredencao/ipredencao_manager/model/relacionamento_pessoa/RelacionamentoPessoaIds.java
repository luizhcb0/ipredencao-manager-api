package org.ipredencao.ipredencao_manager.model.relacionamento_pessoa;

import org.ipredencao.ipredencao_manager.model.TipoRelacionamento;
import org.joda.time.DateTime;

public class RelacionamentoPessoaIds {
    private Long id;
    private Long pessoaId;
    private Long pessoaRelacionadaId;
    private TipoRelacionamento tipoRelacionamento;
    private DateTime inicioRelacionamento;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPessoaId() { return pessoaId; }
    public void setPessoaId(Long pessoaId) { this.pessoaId = pessoaId; }
    public Long getPessoaRelacionadaId() { return pessoaRelacionadaId; }
    public void setPessoaRelacionadaId(Long pessoaRelacionadaId) { this.pessoaRelacionadaId = pessoaRelacionadaId; }
    public TipoRelacionamento getTipoRelacionamento() { return tipoRelacionamento; }
    public void setTipoRelacionamento(TipoRelacionamento tipoRelacionamento) { this.tipoRelacionamento = tipoRelacionamento; }
    public DateTime getInicioRelacionamento() { return inicioRelacionamento; }
    public void setInicioRelacionamento(DateTime inicioRelacionamento) { this.inicioRelacionamento = inicioRelacionamento; }
}