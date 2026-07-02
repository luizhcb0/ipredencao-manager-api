package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link PessoaRepository}, focused on the embedded
 * {@code endereco} returned when the ENDERECO include is active (default).
 */
@Transactional
class PessoaRepositoryIT extends IntegrationTestBase {

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private PessoaService pessoaService;

    @Test
    void find_populatesPessoaIdsOnEndereco_whenPeopleShareAddress() {
        // Same default address (cep + logradouro) is reused by PessoaService.create
        Pessoa first = PessoaFixture.builder(pessoaService)
                .nome("First Resident").sexo(Sexo.MASCULINO).enderecoPadrao().build();
        Pessoa second = PessoaFixture.builder(pessoaService)
                .nome("Second Resident").sexo(Sexo.FEMININO).enderecoPadrao().build();

        List<Pessoa> people = pessoaRepository.find(
                PessoaQuery.builder().ids(List.of(first.getId(), second.getId())).build());

        assertThat(people).hasSize(2);
        for (Pessoa p : people) {
            assertThat(p.getEndereco()).isNotNull();
            assertThat(p.getEndereco().getPessoaIds())
                    .containsExactly(
                            Math.min(first.getId(), second.getId()),
                            Math.max(first.getId(), second.getId()));
        }
    }

    @Test
    void find_returnsNullEndereco_whenPersonHasNoAddress() {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "No Address", Sexo.MASCULINO);

        List<Pessoa> people = pessoaRepository.find(
                PessoaQuery.builder().id(pessoa.getId()).build());

        assertThat(people).hasSize(1);
        assertThat(people.getFirst().getEndereco()).isNull();
    }

    @Test
    void find_populatesAuditFieldsOnEmbeddedEndereco() {
        Pessoa pessoa = PessoaFixture.builder(pessoaService)
                .nome("Audited Resident").sexo(Sexo.MASCULINO).enderecoPadrao().build();

        List<Pessoa> people = pessoaRepository.find(
                PessoaQuery.builder().id(pessoa.getId()).build());

        assertThat(people).hasSize(1);
        var endereco = people.getFirst().getEndereco();
        assertThat(endereco).isNotNull();
        assertThat(endereco.getAddedAt()).isNotNull();
        assertThat(endereco.getUpdatedAt()).isNotNull();
    }
}
