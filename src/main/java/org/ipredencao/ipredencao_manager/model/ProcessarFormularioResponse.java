package org.ipredencao.ipredencao_manager.model;

import java.util.List;

public class ProcessarFormularioResponse {
    private Pessoa pessoaPrincipal;
    private List<RelacionamentoPessoa> relacionamentosCriados;
    private String mensagem;

    public ProcessarFormularioResponse() {}

    public ProcessarFormularioResponse(Pessoa pessoaPrincipal, List<RelacionamentoPessoa> relacionamentosCriados, String mensagem) {
        this.pessoaPrincipal = pessoaPrincipal;
        this.relacionamentosCriados = relacionamentosCriados;
        this.mensagem = mensagem;
    }

    public Pessoa getPessoaPrincipal() {
        return pessoaPrincipal;
    }

    public void setPessoaPrincipal(Pessoa pessoaPrincipal) {
        this.pessoaPrincipal = pessoaPrincipal;
    }

    public List<RelacionamentoPessoa> getRelacionamentosCriados() {
        return relacionamentosCriados;
    }

    public void setRelacionamentosCriados(List<RelacionamentoPessoa> relacionamentosCriados) {
        this.relacionamentosCriados = relacionamentosCriados;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}