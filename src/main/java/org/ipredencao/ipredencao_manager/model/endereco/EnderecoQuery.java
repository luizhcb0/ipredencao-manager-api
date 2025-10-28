package org.ipredencao.ipredencao_manager.model.endereco;

import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;

public class EnderecoQuery {
    private Long id;
    private String cep;
    private String logradouro;
    private String numero;
    private String complemento;
    private PaginationParameters pagination;

    public EnderecoQuery() {}

    private EnderecoQuery(Builder builder) {
        this.id = builder.id;
        this.cep = builder.cep;
        this.logradouro = builder.logradouro;
        this.numero = builder.numero;
        this.complemento = builder.complemento;
        this.pagination = builder.pagination;
    }

    public static class Builder {
        private Long id;
        private String cep;
        private String logradouro;
        private String numero;
        private String complemento;
        private PaginationParameters pagination;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder cep(String cep) {
            this.cep = cep;
            return this;
        }

        public Builder logradouro(String logradouro) {
            this.logradouro = logradouro;
            return this;
        }

        public Builder numero(String numero) {
            this.numero = numero;
            return this;
        }

        public Builder complemento(String complemento) {
            this.complemento = complemento;
            return this;
        }

        public Builder pagination(PaginationParameters pagination) {
            this.pagination = pagination;
            return this;
        }

        public EnderecoQuery build() {
            return new EnderecoQuery(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public String getCep() { return cep; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getComplemento() { return complemento; }
    public PaginationParameters getPagination() { return pagination; }
    
    public void setId(Long id) { this.id = id; }
    public void setCep(String cep) { this.cep = cep; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public void setPagination(PaginationParameters pagination) { this.pagination = pagination; }
}
