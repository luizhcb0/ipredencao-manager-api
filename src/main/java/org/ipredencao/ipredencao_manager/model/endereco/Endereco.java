package org.ipredencao.ipredencao_manager.model.endereco;

import org.joda.time.DateTime;
import java.util.List;

public class Endereco {
    private Long id;
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String coordenadas;
    private DateTime addedAt;
    private DateTime updatedAt;
    private Long updatedByUserId;
    private List<Long> pessoaIds;

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCoordenadas() {
        return coordenadas;
    }

    public void setCoordenadas(String coordenadas) {
        this.coordenadas = coordenadas;
    }

    public DateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(DateTime addedAt) {
        this.addedAt = addedAt;
    }

    public DateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(DateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Long updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public List<Long> getPessoaIds() {
        return pessoaIds;
    }

    public void setPessoaIds(List<Long> pessoaIds) {
        this.pessoaIds = pessoaIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Endereco endereco = (Endereco) o;
        
        if (cep != null ? !cep.equals(endereco.cep) : endereco.cep != null) return false;
        if (logradouro != null ? !logradouro.equals(endereco.logradouro) : endereco.logradouro != null) return false;
        if (numero != null ? !numero.equals(endereco.numero) : endereco.numero != null) return false;
        if (bairro != null ? !bairro.equals(endereco.bairro) : endereco.bairro != null) return false;
        return complemento != null ? complemento.equals(endereco.complemento) : endereco.complemento == null;
    }

    @Override
    public int hashCode() {
        int result = cep != null ? cep.hashCode() : 0;
        result = 31 * result + (logradouro != null ? logradouro.hashCode() : 0);
        result = 31 * result + (numero != null ? numero.hashCode() : 0);
        result = 31 * result + (complemento != null ? complemento.hashCode() : 0);
        result = 31 * result + (bairro != null ? bairro.hashCode() : 0);
        return result;
    }
}

