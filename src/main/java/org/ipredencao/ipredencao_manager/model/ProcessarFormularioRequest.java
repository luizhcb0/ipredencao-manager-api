package org.ipredencao.ipredencao_manager.model;

public class ProcessarFormularioRequest {
    private Long formularioId;
    private Long pessoaId; // ID da pessoa a ser atualizada, null para criar nova

    public ProcessarFormularioRequest() {}

    public ProcessarFormularioRequest(Long formularioId, Long pessoaId) {
        this.formularioId = formularioId;
        this.pessoaId = pessoaId;
    }

    public Long getFormularioId() {
        return formularioId;
    }

    public void setFormularioId(Long formularioId) {
        this.formularioId = formularioId;
    }

    public Long getPessoaId() {
        return pessoaId;
    }

    public void setPessoaId(Long pessoaId) {
        this.pessoaId = pessoaId;
    }
}