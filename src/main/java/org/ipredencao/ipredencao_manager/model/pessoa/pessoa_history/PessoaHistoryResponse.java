package org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history;

import java.util.List;
import java.util.ArrayList;

public class PessoaHistoryResponse {
    private Long pessoaId;
    private List<PessoaHistory> history;

    public PessoaHistoryResponse() {
        this.history = new ArrayList<>();
    }

    public PessoaHistoryResponse(Long pessoaId) {
        this.pessoaId = pessoaId;
        this.history = new ArrayList<>();
    }

    public PessoaHistoryResponse(Long pessoaId, List<PessoaHistory> history) {
        this.pessoaId = pessoaId;
        this.history = history != null ? history : new ArrayList<>();
    }

    public void adicionarEntrada(PessoaHistory history) {
        if (this.history == null) {
            this.history = new ArrayList<>();
        }
        this.history.add(history);
    }

    public boolean temHistorico() {
        return this.history != null && !this.history.isEmpty();
    }

    // Getters e Setters
    public Long getPessoaId() {
        return pessoaId;
    }

    public void setPessoaId(Long pessoaId) {
        this.pessoaId = pessoaId;
    }

    public List<PessoaHistory> getHistory() {
        return history;
    }

    public void setHistory(List<PessoaHistory> history) {
        this.history = history;
    }

    @Override
    public String toString() {
        return "PessoaHistoryResponse{" +
                "pessoaId=" + pessoaId +
                ", history=" + history +
                '}';
    }
}
