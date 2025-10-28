package org.ipredencao.ipredencao_manager.model.formulario_pessoa;

import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil;
import org.ipredencao.ipredencao_manager.model.pessoa.FormPessoaStatus;
import org.ipredencao.ipredencao_manager.model.pessoa.Regiao;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoBatismo;
import org.joda.time.DateTime;
import java.util.List;

public class FormularioPessoaQuery {
    private Long id;
    private List<Long> ids;
    private String nome;
    private String apelido;
    private String email;
    private String telefone;
    private String cpf;
    private String rg;
    private EstadoCivil estadoCivil;
    private String campus;
    private Regiao regiao;
    private DateTime dataNascimentoFrom;
    private DateTime dataNascimentoTo;
    private TipoBatismo tipoBatismo;
    private CategoriaEnum categoria;
    private FormPessoaStatus status;
    private PaginationParameters pagination;

    // Construtor padrão para Jackson
    public FormularioPessoaQuery() {}

    // Construtor privado para usar o builder
    private FormularioPessoaQuery(Builder builder) {
        this.id = builder.id;
        this.ids = builder.ids;
        this.nome = builder.nome;
        this.apelido = builder.apelido;
        this.email = builder.email;
        this.telefone = builder.telefone;
        this.cpf = builder.cpf;
        this.rg = builder.rg;
        this.estadoCivil = builder.estadoCivil;
        this.campus = builder.campus;
        this.regiao = builder.regiao;
        this.dataNascimentoFrom = builder.dataNascimentoFrom;
        this.dataNascimentoTo = builder.dataNascimentoTo;
        this.tipoBatismo = builder.tipoBatismo;
        this.categoria = builder.categoria;
        this.status = builder.status;
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
        private EstadoCivil estadoCivil;
        private String campus;
        private Regiao regiao;
        private DateTime dataNascimentoFrom;
        private DateTime dataNascimentoTo;
        private TipoBatismo tipoBatismo;
        private CategoriaEnum categoria;
        private FormPessoaStatus status;
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

        public Builder estadoCivil(EstadoCivil estadoCivil) {
            this.estadoCivil = estadoCivil;
            return this;
        }

        public Builder campus(String campus) {
            this.campus = campus;
            return this;
        }

        public Builder regiao(Regiao regiao) {
            this.regiao = regiao;
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

        public Builder categoria(CategoriaEnum categoria) {
            this.categoria = categoria;
            return this;
        }

        public Builder status(FormPessoaStatus status) {
            this.status = status;
            return this;
        }

        public Builder pagination(PaginationParameters pagination) {
            this.pagination = pagination;
            return this;
        }

        public FormularioPessoaQuery build() {
            return new FormularioPessoaQuery(this);
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
    public EstadoCivil getEstadoCivil() { return estadoCivil; }
    public String getCampus() { return campus; }
    public Regiao getRegiao() { return regiao; }
    public DateTime getDataNascimentoFrom() { return dataNascimentoFrom; }
    public DateTime getDataNascimentoTo() { return dataNascimentoTo; }
    public TipoBatismo getTipoBatismo() { return tipoBatismo; }
    public CategoriaEnum getCategoria() { return categoria; }
    public FormPessoaStatus getStatus() { return status; }
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
    public void setEstadoCivil(EstadoCivil estadoCivil) { this.estadoCivil = estadoCivil; }
    public void setCampus(String campus) { this.campus = campus; }
    public void setRegiao(Regiao regiao) { this.regiao = regiao; }
    public void setDataNascimentoFrom(DateTime dataNascimentoFrom) { this.dataNascimentoFrom = dataNascimentoFrom; }
    public void setDataNascimentoTo(DateTime dataNascimentoTo) { this.dataNascimentoTo = dataNascimentoTo; }
    public void setTipoBatismo(TipoBatismo tipoBatismo) { this.tipoBatismo = tipoBatismo; }
    public void setCategoria(CategoriaEnum categoria) { this.categoria = categoria; }
    public void setStatus(FormPessoaStatus status) { this.status = status; }
    public void setPagination(PaginationParameters pagination) { this.pagination = pagination; }
}
