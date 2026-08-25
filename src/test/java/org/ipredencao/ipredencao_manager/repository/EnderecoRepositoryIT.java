package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.endereco.EnderecoQuery;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
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
 * Integration tests for {@link EnderecoRepository}, ensuring the batch
 * {@code getPersonIdsFromAddresses} keeps the repository contract: every address returned
 * has pessoaIds populated (empty list when no residents).
 */
@Transactional
class EnderecoRepositoryIT extends IntegrationTestBase {

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private PessoaService pessoaService;

    /** CEP used by {@link PessoaFixture.Builder#enderecoPadrao()}. */
    private static final String FIXTURE_CEP = "50000-000";

    @Test
    void findById_populatesPessoaIds() {
        // PessoaService.create reuses the same address row for both people (same cep + logradouro)
        Pessoa first = PessoaFixture.builder(pessoaService)
                .nome("Resident A").sexo(Sexo.MASCULINO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE).enderecoPadrao().build();
        Pessoa second = PessoaFixture.builder(pessoaService)
                .nome("Resident B").sexo(Sexo.FEMININO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE).enderecoPadrao().build();

        Long addressId = enderecoRepository
                .find(EnderecoQuery.builder().cep(FIXTURE_CEP).build())
                .getFirst().getId();
        Endereco endereco = enderecoRepository.findById(addressId);

        assertThat(endereco).isNotNull();
        assertThat(endereco.getPessoaIds())
                .containsExactly(
                        Math.min(first.getId(), second.getId()),
                        Math.max(first.getId(), second.getId()));
    }

    @Test
    void find_populatesPessoaIds_andReturnsEmptyListForAddressWithoutResidents() {
        Pessoa resident = PessoaFixture.builder(pessoaService)
                .nome("Lone Resident").sexo(Sexo.MASCULINO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE).enderecoPadrao().build();

        Endereco vacant = new Endereco();
        vacant.setCep("60000-000");
        vacant.setLogradouro("Rua Vazia");
        Endereco vacantSaved = enderecoRepository.insert(vacant);
        assertThat(vacantSaved.getPessoaIds()).isEmpty();

        List<Endereco> occupiedResult = enderecoRepository.find(EnderecoQuery.builder().cep(FIXTURE_CEP).build());
        assertThat(occupiedResult).hasSize(1);
        assertThat(occupiedResult.getFirst().getPessoaIds()).containsExactly(resident.getId());

        List<Endereco> vacantResult = enderecoRepository.find(EnderecoQuery.builder().cep("60000-000").build());
        assertThat(vacantResult).hasSize(1);
        assertThat(vacantResult.getFirst().getPessoaIds()).isEmpty();
    }
}
