package org.ipredencao.ipredencao_manager.model;

import org.joda.time.DateTime;
import java.util.List;
import java.util.Optional;

public class PessoaQuery {
    private Optional<Long> id;
    private Optional<List<Long>> ids;
    private Optional<String> nome;
    private Optional<String> apelido;
    private Optional<String> email;
    private Optional<String> telefone;
    private Optional<String> cpf;
    private Optional<String> rg;
    private Optional<EstadoCivil> estadoCivil;
    private Optional<String> campus;
    private Optional<Regiao> regiao;
    private Optional<DateTime> dataNascimentoFrom;
    private Optional<DateTime> dataNascimentoTo;
    private Optional<TipoBatismo> tipoBatismo;
    private Optional<SubcategoriaEnum> subcategoria;

    // Construtor privado para usar o builder
    private PessoaQuery() {
        this.id = Optional.empty();
        this.ids = Optional.empty();
        this.nome = Optional.empty();
        this.apelido = Optional.empty();
        this.email = Optional.empty();
        this.telefone = Optional.empty();
        this.cpf = Optional.empty();
        this.rg = Optional.empty();
        this.estadoCivil = Optional.empty();
        this.campus = Optional.empty();
        this.regiao = Optional.empty();
        this.dataNascimentoFrom = Optional.empty();
        this.dataNascimentoTo = Optional.empty();
        this.tipoBatismo = Optional.empty();
        this.subcategoria = Optional.empty();
    }

    // Builder pattern
    public static class Builder {
        private PessoaQuery query;

        public Builder() {
            this.query = new PessoaQuery();
        }

        public Builder id(Long id) {
            this.query.id = Optional.ofNullable(id);
            return this;
        }

        public Builder ids(List<Long> ids) {
            this.query.ids = Optional.ofNullable(ids);
            return this;
        }

        public Builder nome(String nome) {
            this.query.nome = Optional.ofNullable(nome);
            return this;
        }

        public Builder apelido(String apelido) {
            this.query.apelido = Optional.ofNullable(apelido);
            return this;
        }

        public Builder email(String email) {
            this.query.email = Optional.ofNullable(email);
            return this;
        }

        public Builder telefone(String telefone) {
            this.query.telefone = Optional.ofNullable(telefone);
            return this;
        }

        public Builder cpf(String cpf) {
            this.query.cpf = Optional.ofNullable(cpf);
            return this;
        }

        public Builder rg(String rg) {
            this.query.rg = Optional.ofNullable(rg);
            return this;
        }

        public Builder estadoCivil(EstadoCivil estadoCivil) {
            this.query.estadoCivil = Optional.ofNullable(estadoCivil);
            return this;
        }

        public Builder campus(String campus) {
            this.query.campus = Optional.ofNullable(campus);
            return this;
        }

        public Builder regiao(Regiao regiao) {
            this.query.regiao = Optional.ofNullable(regiao);
            return this;
        }

        public Builder dataNascimentoFrom(DateTime dataNascimentoFrom) {
            this.query.dataNascimentoFrom = Optional.ofNullable(dataNascimentoFrom);
            return this;
        }

        public Builder dataNascimentoTo(DateTime dataNascimentoTo) {
            this.query.dataNascimentoTo = Optional.ofNullable(dataNascimentoTo);
            return this;
        }

        public Builder tipoBatismo(TipoBatismo tipoBatismo) {
            this.query.tipoBatismo = Optional.ofNullable(tipoBatismo);
            return this;
        }

        public Builder subcategoria(SubcategoriaEnum subcategoria) {
            this.query.subcategoria = Optional.ofNullable(subcategoria);
            return this;
        }

        public PessoaQuery build() {
            return this.query;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Optional<Long> getId() { return id; }
    public Optional<List<Long>> getIds() { return ids; }
    public Optional<String> getNome() { return nome; }
    public Optional<String> getApelido() { return apelido; }
    public Optional<String> getEmail() { return email; }
    public Optional<String> getTelefone() { return telefone; }
    public Optional<String> getCpf() { return cpf; }
    public Optional<String> getRg() { return rg; }
    public Optional<EstadoCivil> getEstadoCivil() { return estadoCivil; }
    public Optional<String> getCampus() { return campus; }
    public Optional<Regiao> getRegiao() { return regiao; }
    public Optional<DateTime> getDataNascimentoFrom() { return dataNascimentoFrom; }
    public Optional<DateTime> getDataNascimentoTo() { return dataNascimentoTo; }
    public Optional<TipoBatismo> getTipoBatismo() { return tipoBatismo; }
    public Optional<SubcategoriaEnum> getSubcategoria() { return subcategoria; }
}
