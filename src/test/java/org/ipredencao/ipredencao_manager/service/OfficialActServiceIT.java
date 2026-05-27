package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.controller.form.OfficialActCreateForm;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActQuery;
import org.ipredencao.ipredencao_manager.controller.form.OfficialActUpdateForm;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoBatismo;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.OfficialActFixture;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link OfficialActService}. Covers DTO/metadata validation,
 * fan-out, numero_ordem_admissao assignment/inheritance, per-form effects on {@link Pessoa},
 * the restricted update, the delete-reverts-category flow, and pagination.
 *
 * <p>Most tests are {@link Transactional} and roll back automatically. Tests that require
 * {@code NOW()} to advance between writes (sequence consumption or pessoa_history-based
 * category reversal) are marked {@link Propagation#NOT_SUPPORTED} and may leak rows —
 * acceptable in the ephemeral Postgres container.
 */
@Transactional
class OfficialActServiceIT extends IntegrationTestBase {

    @Autowired private OfficialActService service;
    @Autowired private PessoaService pessoaService;

    // ===== validateCreateDto =====

    @Test
    void create_throwsWhenDtoIsNull() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DTO");
    }

    @Test
    void create_throwsWhenFormIdIsNull() {
        OfficialActCreateForm dto = baseDto(somePessoa("Form null"), OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        dto.setOfficialActFormId(null);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("officialActFormId");
    }

    @Test
    void create_throwsWhenFormIdIsUnknown() {
        OfficialActCreateForm dto = baseDto(somePessoa("Form unknown"), OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        dto.setOfficialActFormId(99999L);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throwsWhenActDateIsNull() {
        OfficialActCreateForm dto = baseDto(somePessoa("Act date null"), OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        dto.setActDate(null);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actDate");
    }

    @Test
    void create_throwsWhenPersonIdsIsEmpty() {
        OfficialActCreateForm dto = baseDto(somePessoa("PersonIds empty"), OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        dto.setPersonIds(List.of());

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("personIds");
    }

    @Test
    void create_throwsWhenPersonIdsContainsNull() {
        Pessoa p = somePessoa("PersonIds contains null");
        OfficialActCreateForm dto = baseDto(p, OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        dto.setPersonIds(java.util.Arrays.asList(p.getId(), null));

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulos");
    }

    // ===== validateMetadata =====

    @Test
    void create_throwsWhenRequiredMetadataIsMissing() {
        // ADM_MC_PROFISSAO_FE requires `celebrante` (PERSON_REF).
        OfficialActCreateForm dto = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(somePessoa("Missing meta").getId())
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .metadata(Map.of())
                .build();

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("celebrante");
    }

    @Test
    void create_throwsWhenRequiredMetadataIsBlank() {
        // PERSON_REF é objeto {id?, name}; `name` em branco deve falhar como "vazio".
        OfficialActCreateForm dto = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(somePessoa("Blank meta").getId())
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .metadata(Map.of("celebrante", Map.of("name", "   ")))
                .build();

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    void create_acceptsNullMetadataWhenAllFieldsAreOptional() {
        // DEM_MC_EXCLUSAO_A_PEDIDO has only one optional field (motivo).
        OfficialActCreateForm dto = OfficialActFixture.builder(OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO)
                .personId(somePessoa("Null meta ok").getId())
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .metadata(null)
                .build();
        dto.setMetadata(null); // builder defaults to metadataMinimo when not set; force null.

        List<OfficialAct> created = service.create(dto);

        assertThat(created).hasSize(1);
        assertThat(created.get(0).getMetadata()).isNotNull(); // service stores empty map, not null
    }

    // ===== fan-out =====

    @Test
    void create_returnsOneActPerPersonInOrder() {
        Pessoa a = somePessoa("Fan A");
        Pessoa b = somePessoa("Fan B");
        OfficialActCreateForm dto = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personIds(List.of(a.getId(), b.getId()))
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .build();

        List<OfficialAct> created = service.create(dto);

        assertThat(created)
                .hasSize(2)
                .extracting(OfficialAct::getPersonId)
                .containsExactly(a.getId(), b.getId());
    }

    @Test
    void create_deduplicatesPersonIdsPreservingOrder() {
        Pessoa a = somePessoa("Dedup A");
        Pessoa b = somePessoa("Dedup B");
        OfficialActCreateForm dto = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personIds(List.of(a.getId(), b.getId(), a.getId(), b.getId()))
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .build();

        List<OfficialAct> created = service.create(dto);

        assertThat(created)
                .hasSize(2)
                .extracting(OfficialAct::getPersonId)
                .containsExactly(a.getId(), b.getId());
    }

    // ===== numero ordem admissão =====

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void create_admissionAssignsSequentialNumeroOrdemAdmissao() {
        Pessoa a = somePessoa("Seq A");
        Pessoa b = somePessoa("Seq B");
        OfficialActCreateForm dto = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personIds(List.of(a.getId(), b.getId()))
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .build();

        List<OfficialAct> created = service.create(dto);

        assertThat(created.get(0).getNumeroOrdemAdmissao()).isNotNull();
        assertThat(created.get(1).getNumeroOrdemAdmissao())
                .isEqualTo(created.get(0).getNumeroOrdemAdmissao() + 1);
    }

    @Test
    void create_dismissalLeavesNumeroOrdemAdmissaoNull() {
        OfficialActCreateForm dto = OfficialActFixture.builder(OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO)
                .personId(somePessoa("Dismissal null seq").getId())
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .build();

        OfficialAct created = service.create(dto).get(0);

        assertThat(created.getNumeroOrdemAdmissao()).isNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void create_promotionMncToMcInheritsNumeroFromPriorAdmission() {
        Pessoa p = somePessoa("Promotion with prior");

        OfficialAct admission = service.create(
                OfficialActFixture.builder(OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA)
                        .personId(p.getId())
                        .actDate(new DateTime(2024, 1, 1, 0, 0))
                        .build()).get(0);

        OfficialAct promotion = service.create(
                OfficialActFixture.builder(OfficialActFormEnum.DEM_MNC_PROFISSAO_FE)
                        .personId(p.getId())
                        .actDate(new DateTime(2024, 6, 1, 0, 0))
                        .build()).get(0);

        assertThat(admission.getNumeroOrdemAdmissao()).isNotNull();
        assertThat(promotion.getNumeroOrdemAdmissao()).isEqualTo(admission.getNumeroOrdemAdmissao());
    }

    @Test
    void create_promotionMncToMcWithoutPriorAdmissionLeavesNumeroNull() {
        OfficialAct promotion = service.create(
                OfficialActFixture.builder(OfficialActFormEnum.DEM_MNC_PROFISSAO_FE)
                        .personId(somePessoa("Promotion no prior").getId())
                        .actDate(new DateTime(2024, 6, 1, 0, 0))
                        .build()).get(0);

        assertThat(promotion.getNumeroOrdemAdmissao()).isNull();
    }

    // ===== effects on Pessoa (create) =====

    @Test
    void create_admissionMcWithBatismoSetsBatismoAndProfissaoDeFe() {
        Pessoa p = somePessoa("MC+Batismo");
        DateTime actDate = new DateTime(2024, 3, 3, 0, 0);

        service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE_E_BATISMO)
                .personId(p.getId())
                .actDate(actDate)
                .build());

        Pessoa reloaded = pessoaService.findById(p.getId());
        assertThat(reloaded.getCategoria()).isEqualTo(CategoriaEnum.MEMBRO_COMUNGANTE);
        assertThat(reloaded.getDataBatismo()).isEqualTo(actDate);
        assertThat(reloaded.getTipoBatismo()).isEqualTo(TipoBatismo.ADULTO);
        assertThat(reloaded.getDataProfissaoDeFe()).isEqualTo(actDate);
    }

    @Test
    void create_admissionMncBatismoSetsCategoryAndBatismoInfantil() {
        Pessoa p = somePessoaInfantil("MNC Batismo");
        DateTime actDate = new DateTime(2024, 3, 3, 0, 0);

        service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA)
                .personId(p.getId())
                .actDate(actDate)
                .build());

        Pessoa reloaded = pessoaService.findById(p.getId());
        assertThat(reloaded.getCategoria()).isEqualTo(CategoriaEnum.MEMBRO_NAO_COMUNGANTE);
        assertThat(reloaded.getDataBatismo()).isEqualTo(actDate);
        assertThat(reloaded.getTipoBatismo()).isEqualTo(TipoBatismo.INFANTIL);
        assertThat(reloaded.getDataProfissaoDeFe()).isNull();
    }

    @Test
    void create_dismissalSetsCategoryToExMembro() {
        Pessoa p = somePessoa("Dismissal");

        service.create(OfficialActFixture.builder(OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO)
                .personId(p.getId())
                .actDate(new DateTime(2024, 4, 1, 0, 0))
                .build());

        Pessoa reloaded = pessoaService.findById(p.getId());
        assertThat(reloaded.getCategoria()).isEqualTo(CategoriaEnum.EX_MEMBRO);
        assertThat(reloaded.getDataFalecimento()).isNull();
    }

    @Test
    void create_falecimentoSetsCategoryExMembroAndDataFalecimento() {
        Pessoa p = somePessoa("Falecimento");
        DateTime actDate = new DateTime(2024, 5, 10, 0, 0);

        service.create(OfficialActFixture.builder(OfficialActFormEnum.DEM_MC_FALECIMENTO)
                .personId(p.getId())
                .actDate(actDate)
                .build());

        Pessoa reloaded = pessoaService.findById(p.getId());
        assertThat(reloaded.getCategoria()).isEqualTo(CategoriaEnum.EX_MEMBRO);
        assertThat(reloaded.getDataFalecimento()).isEqualTo(actDate);
    }

    @Test
    void create_promotionMncProfissaoDeFeSetsCategoryAndDataProfissaoDeFe() {
        Pessoa p = somePessoaInfantil("MNC->MC");
        DateTime actDate = new DateTime(2024, 7, 1, 0, 0);

        service.create(OfficialActFixture.builder(OfficialActFormEnum.DEM_MNC_PROFISSAO_FE)
                .personId(p.getId())
                .actDate(actDate)
                .build());

        Pessoa reloaded = pessoaService.findById(p.getId());
        assertThat(reloaded.getCategoria()).isEqualTo(CategoriaEnum.MEMBRO_COMUNGANTE);
        assertThat(reloaded.getDataProfissaoDeFe()).isEqualTo(actDate);
    }

    // ===== update =====
    // Form, person and actDate stay immutable (mutating them would invalidate
    // numeroOrdemAdmissao and the category side-effects). Everything else — including
    // metadata — is editable.

    @Test
    void update_changesMutableFieldsAndKeepsImmutablesIntact() {
        Pessoa p = somePessoa("Update");
        DateTime actDate = new DateTime(2024, 1, 1, 0, 0);
        OfficialAct created = service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(p.getId())
                .actDate(actDate)
                .minuteNumber("100")
                .minuteDate(new DateTime(2024, 1, 5, 0, 0))
                .notes("orig")
                .build()).get(0);

        OfficialActUpdateForm patch = new OfficialActUpdateForm();
        patch.setMinuteNumber("200");
        patch.setMinuteDate(new DateTime(2024, 2, 1, 0, 0));
        patch.setNotes("changed");

        OfficialAct updated = service.update(created.getId(), patch);

        assertThat(updated.getMinuteNumber()).isEqualTo("200");
        assertThat(updated.getMinuteDate()).isEqualTo(new DateTime(2024, 2, 1, 0, 0));
        assertThat(updated.getNotes()).isEqualTo("changed");
        // Immutable fields stay untouched.
        assertThat(updated.getOfficialActFormId()).isEqualTo(created.getOfficialActFormId());
        assertThat(updated.getPersonId()).isEqualTo(created.getPersonId());
        assertThat(updated.getActDate()).isEqualTo(actDate);
    }

    @Test
    void update_persistsNewMetadataWhenProvided() {
        Pessoa p = somePessoa("Metadata Update");
        OfficialAct created = service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(p.getId())
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .metadata(Map.of("celebrante", Map.of("name", "Rev. Antigo")))
                .build()).get(0);

        OfficialActUpdateForm patch = new OfficialActUpdateForm();
        patch.setMetadata(Map.of("celebrante", Map.of("id", 42, "name", "Rev. Novo")));

        OfficialAct updated = service.update(created.getId(), patch);

        Object celebrante = updated.getMetadata().get("celebrante");
        assertThat(celebrante).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) celebrante).get("name")).isEqualTo("Rev. Novo");
        assertThat(((Map<?, ?>) celebrante).get("id")).isEqualTo(42);
    }

    @Test
    void update_rejectsMetadataMissingRequiredField() {
        Pessoa p = somePessoa("Metadata Invalid");
        OfficialAct created = service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(p.getId())
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .metadata(Map.of("celebrante", Map.of("name", "Rev. Original")))
                .build()).get(0);

        OfficialActUpdateForm patch = new OfficialActUpdateForm();
        patch.setMetadata(Map.of("celebrante", Map.of("name", "   ")));

        assertThatThrownBy(() -> service.update(created.getId(), patch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("celebrante");
    }

    @Test
    void update_preservesMetadataWhenPatchOmitsIt() {
        Pessoa p = somePessoa("Metadata Preserved");
        OfficialAct created = service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(p.getId())
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .metadata(Map.of("celebrante", Map.of("name", "Rev. Mantido")))
                .build()).get(0);

        OfficialActUpdateForm patch = new OfficialActUpdateForm();
        patch.setNotes("only notes changed");

        OfficialAct updated = service.update(created.getId(), patch);

        Object celebrante = updated.getMetadata().get("celebrante");
        assertThat(celebrante).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) celebrante).get("name")).isEqualTo("Rev. Mantido");
    }

    @Test
    void update_throwsWhenIdUnknown() {
        OfficialActUpdateForm patch = new OfficialActUpdateForm();
        patch.setMinuteNumber("anything");

        assertThatThrownBy(() -> service.update(999_999L, patch))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ===== delete (reverts category via pessoa_history) =====
    //
    // These tests run with NOT_SUPPORTED because the trigger-driven added_at on pessoa_history
    // must be *strictly* earlier than the act's added_at — within a single transaction Postgres'
    // NOW() is pinned to txn start, so they'd be equal and findCategoriaIdBefore would find
    // nothing. Cost: rows leak. Container is ephemeral, so this is acceptable.

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void delete_revertsCategoryToPreviousValueViaHistory() throws InterruptedException {
        Pessoa p = somePessoaInfantil("Delete revert"); // starts as MNC
        Thread.sleep(10); // ensure act.added_at > pessoa.added_at

        OfficialAct admission = service.create(
                OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                        .personId(p.getId())
                        .actDate(new DateTime(2024, 1, 1, 0, 0))
                        .build()).get(0);

        assertThat(pessoaService.findById(p.getId()).getCategoria())
                .as("admission promotes MNC -> MC")
                .isEqualTo(CategoriaEnum.MEMBRO_COMUNGANTE);

        service.delete(admission.getId());

        assertThat(pessoaService.findById(p.getId()).getCategoria())
                .as("delete reverts category to MNC")
                .isEqualTo(CategoriaEnum.MEMBRO_NAO_COMUNGANTE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void delete_doesNotRevertDataBatismoOrProfissaoDeFe() throws InterruptedException {
        Pessoa p = somePessoa("Delete data-keep");
        Thread.sleep(10);
        DateTime actDate = new DateTime(2024, 2, 2, 0, 0);

        OfficialAct admission = service.create(
                OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE_E_BATISMO)
                        .personId(p.getId())
                        .actDate(actDate)
                        .build()).get(0);

        service.delete(admission.getId());

        Pessoa reloaded = pessoaService.findById(p.getId());
        // Dates carry historic meaning; only category is reverted.
        assertThat(reloaded.getDataBatismo()).isEqualTo(actDate);
        assertThat(reloaded.getTipoBatismo()).isEqualTo(TipoBatismo.ADULTO);
        assertThat(reloaded.getDataProfissaoDeFe()).isEqualTo(actDate);
    }

    @Test
    void delete_throwsWhenIdUnknown() {
        assertThatThrownBy(() -> service.delete(999_999L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ===== reads =====

    @Test
    void findById_throwsWhenIdUnknown() {
        assertThatThrownBy(() -> service.findById(999_999L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void findPaginated_returnsListAndTotalForPersonFilter() {
        Pessoa p = somePessoa("Pag");
        service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(p.getId())
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .build());
        service.create(OfficialActFixture.builder(OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO)
                .personId(p.getId())
                .actDate(new DateTime(2024, 2, 1, 0, 0))
                .build());

        OfficialActQuery query = new OfficialActQuery();
        query.setPersonId(p.getId());

        PagedResponse<OfficialAct> page = service.findPaginated(query);

        assertThat(page.getPage().getTotal()).isEqualTo(2);
        assertThat(page.getData())
                .hasSize(2)
                .allMatch(act -> act.getPersonId().equals(p.getId()));
    }

    // ===== helpers =====

    private Pessoa somePessoa(String nome) {
        return PessoaFixture.membroComungante(pessoaService, nome, Sexo.MASCULINO);
    }

    private Pessoa somePessoaInfantil(String nome) {
        return PessoaFixture.menorNaoComungante(
                pessoaService,
                nome,
                Sexo.FEMININO,
                new DateTime(2020, 5, 5, 0, 0));
    }

    private OfficialActCreateForm baseDto(Pessoa pessoa, OfficialActFormEnum form) {
        OfficialActCreateForm dto = new OfficialActCreateForm();
        dto.setOfficialActFormId(form.getId());
        dto.setActDate(new DateTime(2024, 1, 1, 0, 0));
        dto.setPersonIds(List.of(pessoa.getId()));
        dto.setMetadata(new HashMap<>(OfficialActFixture.metadataMinimo(form)));
        return dto;
    }
}
