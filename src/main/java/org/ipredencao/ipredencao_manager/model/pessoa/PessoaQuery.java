package org.ipredencao.ipredencao_manager.model.pessoa;

import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.joda.time.DateTime;
import java.util.List;

public class PessoaQuery {
    private Long id;
    private List<Long> ids;
    private String nome;
    private String apelido;
    private String email;
    private String telefone;
    private String cpf;
    private String rg;
    private Sexo sexo;
    private EstadoCivil estadoCivil;
    private String campus;
    private DateTime dataNascimentoFrom;
    private DateTime dataNascimentoTo;
    private TipoBatismo tipoBatismo;
    private List<CategoriaEnum> categorias;
    private Long enderecoId;
    private PaginationParameters pagination;

    // Construtor padrão para Jackson
    public PessoaQuery() {}

    // Construtor privado para usar o builder
    private PessoaQuery(Builder builder) {
        this.id = builder.id;
        this.ids = builder.ids;
        this.nome = builder.nome;
        this.apelido = builder.apelido;
        this.email = builder.email;
        this.telefone = builder.telefone;
        this.cpf = builder.cpf;
        this.rg = builder.rg;
        this.sexo = builder.sexo;
        this.estadoCivil = builder.estadoCivil;
        this.campus = builder.campus;
        this.dataNascimentoFrom = builder.dataNascimentoFrom;
        this.dataNascimentoTo = builder.dataNascimentoTo;
        this.tipoBatismo = builder.tipoBatismo;
        this.categorias = builder.categorias;
        this.enderecoId = builder.enderecoId;
        this.pagination = builder.pagination;
    }

    // Builder pattern
    public static class Builder {
        private Long id;
        private List<Long> ids;
        private String nome;
        private String apelido;
        private String email;
        private String telefone;
        private String cpf;
        private String rg;
        private Sexo sexo;
        private EstadoCivil estadoCivil;
        private String campus;
        private DateTime dataNascimentoFrom;
        private DateTime dataNascimentoTo;
        private TipoBatismo tipoBatismo;
        private List<CategoriaEnum> categorias;
        private Long enderecoId;
        private PaginationParameters pagination;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder ids(List<Long> ids) {
            this.ids = ids;
            return this;
        }

        public Builder nome(String nome) {
            this.nome = nome;
            return this;
        }

        public Builder apelido(String apelido) {
            this.apelido = apelido;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder telefone(String telefone) {
            this.telefone = telefone;
            return this;
        }

        public Builder cpf(String cpf) {
            this.cpf = cpf;
            return this;
        }

        public Builder rg(String rg) {
            this.rg = rg;
            return this;
        }

        public Builder sexo(Sexo sexo) {
            this.sexo = sexo;
            return this;
        }

        public Builder estadoCivil(EstadoCivil estadoCivil) {
            this.estadoCivil = estadoCivil;
            return this;
        }

        public Builder campus(String campus) {
            this.campus = campus;
            return this;
        }

        public Builder dataNascimentoFrom(DateTime dataNascimentoFrom) {
            this.dataNascimentoFrom = dataNascimentoFrom;
            return this;
        }

        public Builder dataNascimentoTo(DateTime dataNascimentoTo) {
            this.dataNascimentoTo = dataNascimentoTo;
            return this;
        }

        public Builder tipoBatismo(TipoBatismo tipoBatismo) {
            this.tipoBatismo = tipoBatismo;
            return this;
        }

        public Builder categorias(List<CategoriaEnum> categorias) {
            this.categorias = categorias;
            return this;
        }

        public Builder enderecoId(Long enderecoId) {
            this.enderecoId = enderecoId;
            return this;
        }

        public Builder pagination(PaginationParameters pagination) {
            this.pagination = pagination;
            return this;
        }

        public PessoaQuery build() {
            return new PessoaQuery(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Long getId() { return id; }
    public List<Long> getIds() { return ids; }
    public String getNome() { return nome; }
    public String getApelido() { return apelido; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public String getCpf() { return cpf; }
    public String getRg() { return rg; }
    public Sexo getSexo() { return sexo; }
    public EstadoCivil getEstadoCivil() { return estadoCivil; }
    public String getCampus() { return campus; }
    public DateTime getDataNascimentoFrom() { return dataNascimentoFrom; }
    public DateTime getDataNascimentoTo() { return dataNascimentoTo; }
    public TipoBatismo getTipoBatismo() { return tipoBatismo; }
    public List<CategoriaEnum> getCategorias() { return categorias; }
    public Long getEnderecoId() { return enderecoId; }
    public PaginationParameters getPagination() { return pagination; }

    // Setters para Jackson
    public void setId(Long id) { this.id = id; }
    public void setIds(List<Long> ids) { this.ids = ids; }
    public void setNome(String nome) { this.nome = nome; }
    public void setApelido(String apelido) { this.apelido = apelido; }
    public void setEmail(String email) { this.email = email; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public void setRg(String rg) { this.rg = rg; }
    public void setSexo(Sexo sexo) { this.sexo = sexo; }
    public void setEstadoCivil(EstadoCivil estadoCivil) { this.estadoCivil = estadoCivil; }
    public void setCampus(String campus) { this.campus = campus; }
    public void setDataNascimentoFrom(DateTime dataNascimentoFrom) { this.dataNascimentoFrom = dataNascimentoFrom; }
    public void setDataNascimentoTo(DateTime dataNascimentoTo) { this.dataNascimentoTo = dataNascimentoTo; }
    public void setTipoBatismo(TipoBatismo tipoBatismo) { this.tipoBatismo = tipoBatismo; }
    public void setCategorias(List<CategoriaEnum> categorias) { this.categorias = categorias; }
    public void setEnderecoId(Long enderecoId) { this.enderecoId = enderecoId; }
    public void setPagination(PaginationParameters pagination) { this.pagination = pagination; }
}
