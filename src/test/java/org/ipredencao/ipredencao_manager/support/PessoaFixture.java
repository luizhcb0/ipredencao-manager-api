package org.ipredencao.ipredencao_manager.support;

import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.joda.time.DateTime;

/** Builds and persists {@link Pessoa} instances for integration tests. */
public final class PessoaFixture {

    private PessoaFixture() {}

    public static Builder builder(PessoaService pessoaService) {
        return new Builder(pessoaService);
    }

    public static Pessoa membroComungante(PessoaService pessoaService, String nome, Sexo sexo) {
        return builder(pessoaService)
                .nome(nome)
                .sexo(sexo)
                .dataNascimento(new DateTime(1985, 1, 1, 0, 0))
                .estadoCivil(EstadoCivil.SOLTEIRO_SEM_RELACIONAMENTO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
                .build();
    }

    public static Pessoa menorNaoComungante(PessoaService pessoaService, String nome, Sexo sexo, DateTime dataNascimento) {
        return builder(pessoaService)
                .nome(nome)
                .sexo(sexo)
                .dataNascimento(dataNascimento)
                .estadoCivil(EstadoCivil.SOLTEIRO_SEM_RELACIONAMENTO)
                .categoria(CategoriaEnum.MEMBRO_NAO_COMUNGANTE)
                .build();
    }

    public static final class Builder {
        private final PessoaService pessoaService;
        private final Pessoa pessoa = new Pessoa();

        private Builder(PessoaService pessoaService) {
            this.pessoaService = pessoaService;
        }

        public Builder nome(String nome) { pessoa.setNome(nome); return this; }
        public Builder apelido(String apelido) { pessoa.setApelido(apelido); return this; }
        public Builder email(String email) { pessoa.setEmail(email); return this; }
        public Builder telefone(String telefone) { pessoa.setTelefone(telefone); return this; }
        public Builder sexo(Sexo sexo) { pessoa.setSexo(sexo); return this; }
        public Builder dataNascimento(DateTime dataNascimento) { pessoa.setDataNascimento(dataNascimento); return this; }
        public Builder estadoCivil(EstadoCivil estadoCivil) { pessoa.setEstadoCivil(estadoCivil); return this; }
        public Builder categoria(CategoriaEnum categoria) { pessoa.setCategoria(categoria); return this; }
        public Builder endereco(Endereco endereco) { pessoa.setEndereco(endereco); return this; }
        public Builder dataBatismo(DateTime dataBatismo) { pessoa.setDataBatismo(dataBatismo); return this; }
        public Builder dataProfissaoDeFe(DateTime dataProfissaoDeFe) { pessoa.setDataProfissaoDeFe(dataProfissaoDeFe); return this; }
        public Builder igrejaAnterior(String igrejaAnterior) { pessoa.setIgrejaAnterior(igrejaAnterior); return this; }

        public Builder enderecoPadrao() {
            Endereco e = new Endereco();
            e.setCep("50000-000");
            e.setLogradouro("Rua Teste");
            e.setNumero("100");
            e.setBairro("Boa Vista");
            e.setCidade("Recife");
            e.setEstado("PE");
            pessoa.setEndereco(e);
            return this;
        }

        public Pessoa build() {
            return pessoaService.create(pessoa);
        }
    }
}
