package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActQuery;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OfficialActRepository}. Each test inserts its own data and
 * relies on class-level {@link Transactional} rollback for isolation. The sequence test is
 * marked {@code Propagation.NOT_SUPPORTED} because {@code nextval} survives rollback.
 */
@Transactional
class OfficialActRepositoryIT extends IntegrationTestBase {

    @Autowired
    private OfficialActRepository repo;

    @Autowired
    private PessoaService pessoaService;

    @Test
    void insert_persistsAllFieldsAndAssignsId() {
        Pessoa pessoa = somePessoa("Inserted Person");
        DateTime actDate = new DateTime(2024, 11, 17, 0, 0);
        OfficialAct act = newAct(pessoa, OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA);
        act.setActDate(actDate);
        act.setMinuteNumber("298");
        act.setMinuteDate(actDate);
        act.setNumeroOrdemAdmissao(42L);
        act.setMetadata(Map.of("celebrante", Map.of("name", "Rev. Fulano")));
        act.setNotes("Some notes");

        OfficialAct saved = repo.insert(act);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOfficialActFormId()).isEqualTo(OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA.getId());
        assertThat(saved.getPersonId()).isEqualTo(pessoa.getId());
        assertThat(saved.getActDate()).isEqualTo(actDate);
        assertThat(saved.getMinuteNumber()).isEqualTo("298");
        assertThat(saved.getMinuteDate()).isEqualTo(actDate);
        assertThat(saved.getNumeroOrdemAdmissao()).isEqualTo(42L);
        assertThat(saved.getNotes()).isEqualTo("Some notes");
        assertThat(saved.getAddedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void insert_hydratesDerivedFieldsFromJoins() {
        Pessoa pessoa = somePessoa("Joined Person");
        OfficialAct act = newAct(pessoa, OfficialActFormEnum.ADM_MC_PROFISSAO_FE);

        OfficialAct saved = repo.insert(act);

        assertThat(saved.getOfficialActTypeId())
                .isEqualTo(OfficialActFormEnum.ADM_MC_PROFISSAO_FE.getType().getId());
        assertThat(saved.getFormName()).isEqualTo("Profissão de fé");
        assertThat(saved.getArticleClause()).isEqualTo("Art. 16, a");
        assertThat(saved.getTypeName()).isEqualTo("Admissão de membro comungante");
        assertThat(saved.getCategory()).isEqualTo("ADMISSAO");
        assertThat(saved.getPersonName()).isEqualTo("Joined Person");
    }

    @Test
    void insert_metadataRoundTripsThroughJsonbPreservingTypes() {
        Pessoa pessoa = somePessoa("Metadata Person");
        OfficialAct act = newAct(pessoa, OfficialActFormEnum.ADM_MC_CARTA_TRANSFERENCIA);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("igrejaOrigem", "IPB Centro");
        metadata.put("numeroCarta", "123/2024");
        metadata.put("anoEmissao", 2024);

        act.setMetadata(metadata);
        OfficialAct saved = repo.insert(act);
        OfficialAct reloaded = repo.findById(saved.getId());

        assertThat(reloaded.getMetadata())
                .containsEntry("igrejaOrigem", "IPB Centro")
                .containsEntry("numeroCarta", "123/2024")
                .containsEntry("anoEmissao", 2024);
    }

    @Test
    void insert_nullMetadataIsStoredAsEmptyMap() {
        Pessoa pessoa = somePessoa("Null Metadata Person");
        OfficialAct act = newAct(pessoa, OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        act.setMetadata(null);

        OfficialAct saved = repo.insert(act);

        assertThat(saved.getMetadata()).isNotNull().isEmpty();
    }

    @Test
    void findById_nullOrUnknownIdReturnsNull() {
        assertThat(repo.findById(null)).isNull();
        assertThat(repo.findById(9_999_999L)).isNull();
    }

    @Test
    void findByMinuteNumber_ordersByTypeIdThenFormIdThenActDateThenId() {
        Pessoa pessoa = somePessoa("Sort Person");
        String minute = "299";

        // Insert intentionally out of order — repo must reorder by (type_id, form_id, act_date, id).
        OfficialAct demLate = insertAct(pessoa, OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO, minute, new DateTime(2024, 11, 5, 0, 0));
        OfficialAct demEarly = insertAct(pessoa, OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO, minute, new DateTime(2024, 11, 1, 0, 0));
        OfficialAct admEarly = insertAct(pessoa, OfficialActFormEnum.ADM_MC_PROFISSAO_FE, minute, new DateTime(2024, 11, 3, 0, 0));
        OfficialAct admLate = insertAct(pessoa, OfficialActFormEnum.ADM_MC_PROFISSAO_FE, minute, new DateTime(2024, 11, 7, 0, 0));

        List<OfficialAct> acts = repo.findByMinuteNumber(minute);

        assertThat(acts).extracting(OfficialAct::getId)
                .containsExactly(admEarly.getId(), admLate.getId(), demEarly.getId(), demLate.getId());
    }

    @Test
    void findByMinuteNumber_blankOrNullReturnsEmptyList() {
        assertThat(repo.findByMinuteNumber(null)).isEmpty();
        assertThat(repo.findByMinuteNumber("")).isEmpty();
        assertThat(repo.findByMinuteNumber("   ")).isEmpty();
    }

    @Test
    void findByPersonId_ordersByActDateAndIdDescending() {
        Pessoa pessoa = somePessoa("History Person");

        OfficialAct older = insertAct(pessoa, OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA, "300", new DateTime(2020, 1, 1, 0, 0));
        OfficialAct middle = insertAct(pessoa, OfficialActFormEnum.ADM_MC_PROFISSAO_FE,     "310", new DateTime(2024, 6, 1, 0, 0));
        OfficialAct newer = insertAct(pessoa, OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO, "320", new DateTime(2025, 1, 1, 0, 0));

        List<OfficialAct> acts = repo.findByPersonId(pessoa.getId());

        assertThat(acts).extracting(OfficialAct::getId)
                .containsExactly(newer.getId(), middle.getId(), older.getId());
    }

    @Test
    void findByPersonId_nullReturnsEmptyList() {
        assertThat(repo.findByPersonId(null)).isEmpty();
    }

    /**
     * Runs outside the class-level transaction because {@code NOW()} in Postgres is
     * pinned to the transaction start — insert and update would otherwise share the
     * same timestamp and we could not observe the trigger bumping {@code updated_at}.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void update_modifiesMutableFieldsAndBumpsUpdatedAt() throws InterruptedException {
        Pessoa pessoa = somePessoa("Update Person");
        OfficialAct saved = repo.insert(newAct(pessoa, OfficialActFormEnum.ADM_MC_PROFISSAO_FE));
        try {
            DateTime originalUpdatedAt = saved.getUpdatedAt();
            Thread.sleep(10);

            saved.setMinuteNumber("999");
            saved.setMinuteDate(new DateTime(2025, 3, 1, 0, 0));
            saved.setNotes("changed");
            saved.setMetadata(Map.of("celebrante", Map.of("name", "Rev. Updated")));
            OfficialAct updated = repo.update(saved);

            assertThat(updated.getMinuteNumber()).isEqualTo("999");
            assertThat(updated.getMinuteDate()).isEqualTo(new DateTime(2025, 3, 1, 0, 0));
            assertThat(updated.getNotes()).isEqualTo("changed");
            assertThat(updated.getMetadata()).containsKey("celebrante");
            assertThat(((Map<?, ?>) updated.getMetadata().get("celebrante")).get("name"))
                    .isEqualTo("Rev. Updated");
            assertThat(updated.getUpdatedAt().isAfter(originalUpdatedAt))
                    .as("updatedAt should be bumped by trigger_official_act_updated_at")
                    .isTrue();
        } finally {
            repo.delete(saved.getId());
        }
    }

    @Test
    void delete_removesRowAndSubsequentFindReturnsNull() {
        Pessoa pessoa = somePessoa("Delete Person");
        OfficialAct saved = repo.insert(newAct(pessoa, OfficialActFormEnum.ADM_MC_PROFISSAO_FE));

        repo.delete(saved.getId());

        assertThat(repo.findById(saved.getId())).isNull();
    }

    @Test
    void findLatestNumeroOrdemAdmissao_returnsMostRecentNonNullValue() {
        Pessoa pessoa = somePessoa("Numero Person");

        OfficialAct first = newAct(pessoa, OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA);
        first.setActDate(new DateTime(2010, 5, 1, 0, 0));
        first.setNumeroOrdemAdmissao(50L);
        repo.insert(first);

        OfficialAct second = newAct(pessoa, OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        second.setActDate(new DateTime(2024, 11, 1, 0, 0));
        second.setNumeroOrdemAdmissao(50L); // promotion inherits the same number
        repo.insert(second);

        // Demissão posterior sem numero_ordem_admissao deve ser ignorada pelo query.
        OfficialAct ignored = newAct(pessoa, OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO);
        ignored.setActDate(new DateTime(2025, 1, 1, 0, 0));
        ignored.setNumeroOrdemAdmissao(null);
        repo.insert(ignored);

        Optional<Long> latest = repo.findLatestNumeroOrdemAdmissao(pessoa.getId());

        assertThat(latest).contains(50L);
    }

    @Test
    void findLatestNumeroOrdemAdmissao_returnsEmptyWhenAllNumerosAreNull() {
        Pessoa pessoa = somePessoa("All Null Person");
        OfficialAct act = newAct(pessoa, OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO);
        act.setNumeroOrdemAdmissao(null);
        repo.insert(act);

        assertThat(repo.findLatestNumeroOrdemAdmissao(pessoa.getId())).isEmpty();
    }

    @Test
    void findLatestNumeroOrdemAdmissao_nullPersonIdReturnsEmpty() {
        assertThat(repo.findLatestNumeroOrdemAdmissao(null)).isEmpty();
    }

    @Test
    void findByQuery_filtersByMinuteNumberAndAppliesPagination() {
        Pessoa pessoa = somePessoa("Query Person");
        for (int i = 0; i < 5; i++) {
            insertAct(pessoa, OfficialActFormEnum.ADM_MC_PROFISSAO_FE, "400", new DateTime(2024, 1, 1, 0, 0).plusDays(i));
        }
        insertAct(pessoa, OfficialActFormEnum.ADM_MC_PROFISSAO_FE, "OTHER", new DateTime(2024, 6, 1, 0, 0));

        OfficialActQuery query = new OfficialActQuery();
        query.setMinuteNumber("400");
        query.setPagination(new PaginationParameters(2, 0));

        List<OfficialAct> page1 = repo.findByQuery(query);
        int total = repo.count(query);

        assertThat(page1).hasSize(2);
        assertThat(page1).extracting(OfficialAct::getMinuteNumber).containsOnly("400");
        assertThat(total).isEqualTo(5);
    }

    @Test
    void findByQuery_filtersByFormIdsAndPersonId() {
        Pessoa target = somePessoa("Target Person");
        Pessoa other = somePessoa("Other Person");
        insertAct(target, OfficialActFormEnum.ADM_MC_PROFISSAO_FE,    "500", new DateTime(2024, 1, 1, 0, 0));
        insertAct(target, OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO, "500", new DateTime(2024, 2, 1, 0, 0));
        insertAct(other,  OfficialActFormEnum.ADM_MC_PROFISSAO_FE,    "500", new DateTime(2024, 3, 1, 0, 0));

        OfficialActQuery query = new OfficialActQuery();
        query.setPersonId(target.getId());
        query.setFormIds(List.of(OfficialActFormEnum.ADM_MC_PROFISSAO_FE.getId()));

        List<OfficialAct> results = repo.findByQuery(query);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getPersonId()).isEqualTo(target.getId());
        assertThat(results.get(0).getOfficialActFormId())
                .isEqualTo(OfficialActFormEnum.ADM_MC_PROFISSAO_FE.getId());
    }

    /**
     * Sequence values escape transaction rollback, so this test runs outside the class-level
     * transaction. We manually delete what we inserted in {@code @AfterEach} via direct DSL.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void nextNumeroOrdemAdmissao_isMonotonicAcrossCalls() {
        long first = repo.nextNumeroOrdemAdmissao();
        long second = repo.nextNumeroOrdemAdmissao();
        long third = repo.nextNumeroOrdemAdmissao();

        assertThat(second).isEqualTo(first + 1);
        assertThat(third).isEqualTo(second + 1);
    }

    // ---------------------------------------------------------------- helpers

    private Pessoa somePessoa(String nome) {
        return PessoaFixture.membroComungante(pessoaService, nome, Sexo.MASCULINO);
    }

    private OfficialAct newAct(Pessoa pessoa, OfficialActFormEnum form) {
        OfficialAct act = new OfficialAct();
        act.setOfficialActFormId(form.getId());
        act.setPersonId(pessoa.getId());
        act.setActDate(new DateTime(2024, 1, 1, 0, 0));
        act.setMetadata(new HashMap<>());
        return act;
    }

    private OfficialAct insertAct(Pessoa pessoa, OfficialActFormEnum form, String minute, DateTime actDate) {
        OfficialAct act = newAct(pessoa, form);
        act.setActDate(actDate);
        act.setMinuteNumber(minute);
        return repo.insert(act);
    }
}
