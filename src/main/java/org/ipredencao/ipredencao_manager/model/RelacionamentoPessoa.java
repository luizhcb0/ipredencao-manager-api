package org.ipredencao.ipredencao_manager.model;

public class RelacionamentoPessoa {
    private Long id;
    private Pessoa pessoaRelacionada;
    private String tipoRelacionamento; // Ex: "responsavel", "conjuge", "filho", etc

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Pessoa getPessoaRelacionada() { return pessoaRelacionada; }
    public void setPessoaRelacionada(Pessoa pessoaRelacionada) { this.pessoaRelacionada = pessoaRelacionada; }
    public String getTipoRelacionamento() { return tipoRelacionamento; }
    public void setTipoRelacionamento(String tipoRelacionamento) { this.tipoRelacionamento = tipoRelacionamento; }
} 