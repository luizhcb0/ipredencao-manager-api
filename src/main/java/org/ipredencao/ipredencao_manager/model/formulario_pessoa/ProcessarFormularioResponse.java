package org.ipredencao.ipredencao_manager.model.formulario_pessoa;

import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import java.util.List;

public class ProcessarFormularioResponse {
    private Pessoa pessoaPrincipal;
    private List<Relacionamento> relacionamentosCriados;
    private String mensagem;

    public ProcessarFormularioResponse() {}

    public ProcessarFormularioResponse(Pessoa pessoaPrincipal, List<Relacionamento> relacionamentosCriados, String mensagem) {
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

    public List<Relacionamento> getRelacionamentosCriados() {
        return relacionamentosCriados;
    }

    public void setRelacionamentosCriados(List<Relacionamento> relacionamentosCriados) {
        this.relacionamentosCriados = relacionamentosCriados;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}