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
import org.joda.time.LocalDate;
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
 * Integration tests for {@link OfficialActService}. Covers Form/metadata validation,
 * fan-out, admission_order_number assignment/inheritance, per-form effects on {@link Pessoa},
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

    // ===== validateCreateForm =====

    @Test
    void create_throwsWhenFormIsNull() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Form");
    }

    @Test
    void create_throwsWhenFormIdIsNull() {
        OfficialActCreateForm form = baseForm(somePessoa("Form null"), OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        form.setOfficialActFormId(null);

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("officialActFormId");
    }

    @Test
    void create_throwsWhenFormIdIsUnknown() {
        OfficialActCreateForm form = baseForm(somePessoa("Form unknown"), OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        form.setOfficialActFormId(99999L);

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throwsWhenActDateIsNull() {
        OfficialActCreateForm form = baseForm(somePessoa("Act date null"), OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        form.setActDate(null);

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actDate");
    }

    @Test
    void create_throwsWhenPersonIdsIsEmpty() {
        OfficialActCreateForm form = baseForm(somePessoa("PersonIds empty"), OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        form.setPersonIds(List.of());

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("personIds");
    }

    @Test
    void create_throwsWhenPersonIdsContainsNull() {
        Pessoa p = somePessoa("PersonIds contains null");
        OfficialActCreateForm form = baseForm(p, OfficialActFormEnum.ADM_MC_PROFISSAO_FE);
        form.setPersonIds(java.util.Arrays.asList(p.getId(), null));

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nulos");
    }

    // ===== validateMetadata =====

    @Test
    void create_throwsWhenRequiredMetadataIsMissing() {
        // ADM_MC_PROFISSAO_FE requires `celebrant` (PERSON_REF).
        OfficialActCreateForm form = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(somePessoa("Missing meta").getId())
                .actDate(new LocalDate(2024, 1, 1))
                .metadata(Map.of())
                .build();

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("celebrant");
    }

    @Test
    void create_throwsWhenRequiredMetadataIsBlank() {
        // PERSON_REF é objeto {id?, name}; `name` em branco deve falhar como "vazio".
        OfficialActCreateForm form = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(somePessoa("Blank meta").getId())
                .actDate(new LocalDate(2024, 1, 1))
                .metadata(Map.of("celebrant", Map.of("name", "   ")))
                .build();

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vazio");
    }

    @Test
    void create_throwsWhenPersonRefIdIsNotNumber() {
        // PERSON_REF accepts {id?, name}; if `id` is present it must be numeric.
        OfficialActCreateForm form = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(somePessoa("Bad id type").getId())
                .actDate(new LocalDate(2024, 1, 1))
                .metadata(Map.of("celebrant", Map.of("id", "not-a-number", "name", "Fulano")))
                .build();

        assertThatThrownBy(() -> service.create(form))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ID inválido");
    }

    @Test
    void create_acceptsNullMetadataWhenAllFieldsAreOptional() {
        // DEM_MC_EXCLUSAO_A_PEDIDO has only one optional field (reason).
        OfficialActCreateForm form = OfficialActFixture.builder(OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO)
                .personId(somePessoa("Null meta ok").getId())
                .actDate(new LocalDate(2024, 1, 1))
                .metadata(null)
                .build();
        form.setMetadata(null); // builder defaults to metadataMinimo when not set; force null.

        List<OfficialAct> created = service.create(form);

        assertThat(created).hasSize(1);
        assertThat(created.get(0).getMetadata()).isNotNull(); // service stores empty map, not null
    }

    // ===== fan-out =====

    @Test
    void create_returnsOneActPerPersonInOrder() {
        Pessoa a = somePessoa("Fan A");
        Pessoa b = somePessoa("Fan B");
        OfficialActCreateForm form = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personIds(List.of(a.getId(), b.getId()))
                .actDate(new LocalDate(2024, 1, 1))
                .build();

        List<OfficialAct> created = service.create(form);

        assertThat(created)
                .hasSize(2)
                .extracting(OfficialAct::getPersonId)
                .containsExactly(a.getId(), b.getId());
    }

    @Test
    void create_deduplicatesPersonIdsPreservingOrder() {
        Pessoa a = somePessoa("Dedup A");
        Pessoa b = somePessoa("Dedup B");
        OfficialActCreateForm form = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personIds(List.of(a.getId(), b.getId(), a.getId(), b.getId()))
                .actDate(new LocalDate(2024, 1, 1))
                .build();

        List<OfficialAct> created = service.create(form);

        assertThat(created)
                .hasSize(2)
                .extracting(OfficialAct::getPersonId)
                .containsExactly(a.getId(), b.getId());
    }

    // ===== numero ordem admissão =====

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void create_admissionAssignsSequentialAdmissionOrderNumber() {
        Pessoa a = somePessoa("Seq A");
        Pessoa b = somePessoa("Seq B");
        OfficialActCreateForm form = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personIds(List.of(a.getId(), b.getId()))
                .actDate(new LocalDate(2024, 1, 1))
                .build();

        List<OfficialAct> created = service.create(form);

        assertThat(created.get(0).getAdmissionOrderNumber()).isNotNull();
        assertThat(created.get(1).getAdmissionOrderNumber())
                .isEqualTo(created.get(0).getAdmissionOrderNumber() + 1);
    }

    @Test
    void create_dismissalLeavesAdmissionOrderNumberNull() {
        OfficialActCreateForm form = OfficialActFixture.builder(OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO)
                .personId(somePessoa("Dismissal null seq").getId())
                .actDate(new LocalDate(2024, 1, 1))
                .build();

        OfficialAct created = service.create(form).get(0);

        assertThat(created.getAdmissionOrderNumber()).isNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void create_promotionMncToMcInheritsNumeroFromPriorAdmission() {
        Pessoa p = somePessoa("Promotion with prior");

        OfficialAct admission = service.create(
                OfficialActFixture.builder(OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA)
                        .personId(p.getId())
                        .actDate(new LocalDate(2024, 1, 1))
                        .build()).get(0);

        OfficialAct promotion = service.create(
                OfficialActFixture.builder(OfficialActFormEnum.DEM_MNC_PROFISSAO_FE)
                        .personId(p.getId())
                        .actDate(new LocalDate(2024, 6, 1))
                        .build()).get(0);

        assertThat(admission.getAdmissionOrderNumber()).isNotNull();
        assertThat(promotion.getAdmissionOrderNumber()).isEqualTo(admission.getAdmissionOrderNumber());
    }

    @Test
    void create_promotionMncToMcWithoutPriorAdmissionLeavesNumeroNull() {
        OfficialAct promotion = service.create(
                OfficialActFixture.builder(OfficialActFormEnum.DEM_MNC_PROFISSAO_FE)
                        .personId(somePessoa("Promotion no prior").getId())
                        .actDate(new LocalDate(2024, 6, 1))
                        .build()).get(0);

        assertThat(promotion.getAdmissionOrderNumber()).isNull();
    }

    // ===== effects on Pessoa (create) =====

    @Test
    void create_admissionMcWithBatismoSetsBatismoAndProfissaoDeFe() {
        Pessoa p = somePessoa("MC+Batismo");
        LocalDate actDate = new LocalDate(2024, 3, 3);

        service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE_E_BATISMO)
                .personId(p.getId())
                .actDate(actDate)
                .build());

        Pessoa reloaded = pessoaService.findById(p.getId());
        assertThat(reloaded.getCategoria()).isEqualTo(CategoriaEnum.MEMBRO_COMUNGANTE);
        assertThat(reloaded.getDataBatismo()).isEqualTo(actDate.toDateTimeAtStartOfDay());
        assertThat(reloaded.getTipoBatismo()).isEqualTo(TipoBatismo.ADULTO);
        assertThat(reloaded.getDataProfissaoDeFe()).isEqualTo(actDate.toDateTimeAtStartOfDay());
    }

    @Test
    void create_admissionMncBatismoSetsCategoryAndBatismoInfantil() {
        Pessoa p = somePessoaInfantil("MNC Batismo");
        LocalDate actDate = new LocalDate(2024, 3, 3);

        service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA)
                .personId(p.getId())
                .actDate(actDate)
                .build());

        Pessoa reloaded = pessoaService.findById(p.getId());
        assertThat(reloaded.getCategoria()).isEqualTo(CategoriaEnum.MEMBRO_NAO_COMUNGANTE);
        assertThat(reloaded.getDataBatismo()).isEqualTo(actDate.toDateTimeAtStartOfDay());
        assertThat(reloaded.getTipoBatismo()).isEqualTo(TipoBatismo.INFANTIL);
        assertThat(reloaded.getDataProfissaoDeFe()).isNull();
    }

    @Test
    void create_dismissalSetsCategoryToExMembro() {
        Pessoa p = somePessoa("Dismissal");

        service.create(OfficialActFixture.builder(OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO)
                .personId(p.getId())
                .actDate(new LocalDate(2024, 4, 1))
                .build());

        Pessoa reloaded = pessoaService.findById(p.getId());
        assertThat(reloaded.getCategoria()).isEqualTo(CategoriaEnum.EX_MEMBRO);
        assertThat(reloaded.getDataFalecimento()).isNull();
    }

    @Test
    void create_falecimentoSetsCategoryExMembroAndDataFalecimento() {
        Pessoa p = somePessoa("Falecimento");
        LocalDate actDate = new LocalDate(2024, 5, 10);

        service.create(OfficialActFixture.builder(OfficialActFormEnum.DEM_MC_FALECIMENTO)
                .personId(p.getId())
                .actDate(actDate)
                .build());

        Pessoa reloaded = pessoaService.findById(p.getId());
        assertThat(reloaded.getCategoria()).isEqualTo(CategoriaEnum.EX_MEMBRO);
        assertThat(reloaded.getDataFalecimento()).isEqualTo(actDate.toDateTimeAtStartOfDay());
    }

    @Test
    void create_promotionMncProfissaoDeFeSetsCategoryAndDataProfissaoDeFe() {
        Pessoa p = somePessoaInfantil("MNC->MC");
        LocalDate actDate = new LocalDate(2024, 7, 1);

        service.create(OfficialActFixture.builder(OfficialActFormEnum.DEM_MNC_PROFISSAO_FE)
                .personId(p.getId())
                .actDate(actDate)
                .build());

        Pessoa reloaded = pessoaService.findById(p.getId());
        assertThat(reloaded.getCategoria()).isEqualTo(CategoriaEnum.MEMBRO_COMUNGANTE);
        assertThat(reloaded.getDataProfissaoDeFe()).isEqualTo(actDate.toDateTimeAtStartOfDay());
    }

    // ===== update =====
    // Form, person and actDate stay immutable (mutating them would invalidate
    // admissionOrderNumber and the category side-effects). Everything else — including
    // metadata — is editable.

    @Test
    void update_changesMutableFieldsAndKeepsImmutablesIntact() {
        Pessoa p = somePessoa("Update");
        LocalDate actDate = new LocalDate(2024, 1, 1);
        OfficialAct created = service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(p.getId())
                .actDate(actDate)
                .minuteNumber("100")
                .minuteDate(new LocalDate(2024, 1, 5))
                .notes("orig")
                .build()).get(0);

        OfficialActUpdateForm patch = new OfficialActUpdateForm();
        patch.setMinuteNumber("200");
        patch.setMinuteDate(new LocalDate(2024, 2, 1));
        patch.setNotes("changed");

        OfficialAct updated = service.update(created.getId(), patch);

        assertThat(updated.getMinuteNumber()).isEqualTo("200");
        assertThat(updated.getMinuteDate()).isEqualTo(new LocalDate(2024, 2, 1));
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
                .actDate(new LocalDate(2024, 1, 1))
                .metadata(Map.of("celebrant", Map.of("name", "Rev. Antigo")))
                .build()).get(0);

        OfficialActUpdateForm patch = new OfficialActUpdateForm();
        patch.setMetadata(Map.of("celebrant", Map.of("id", 42, "name", "Rev. Novo")));

        OfficialAct updated = service.update(created.getId(), patch);

        Object celebrant = updated.getMetadata().get("celebrant");
        assertThat(celebrant).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) celebrant).get("name")).isEqualTo("Rev. Novo");
        assertThat(((Map<?, ?>) celebrant).get("id")).isEqualTo(42);
    }

    @Test
    void update_rejectsMetadataMissingRequiredField() {
        Pessoa p = somePessoa("Metadata Invalid");
        OfficialAct created = service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(p.getId())
                .actDate(new LocalDate(2024, 1, 1))
                .metadata(Map.of("celebrant", Map.of("name", "Rev. Original")))
                .build()).get(0);

        OfficialActUpdateForm patch = new OfficialActUpdateForm();
        patch.setMetadata(Map.of("celebrant", Map.of("name", "   ")));

        assertThatThrownBy(() -> service.update(created.getId(), patch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("celebrant");
    }

    @Test
    void update_preservesMetadataWhenPatchOmitsIt() {
        Pessoa p = somePessoa("Metadata Preserved");
        OfficialAct created = service.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(p.getId())
                .actDate(new LocalDate(2024, 1, 1))
                .metadata(Map.of("celebrant", Map.of("name", "Rev. Mantido")))
                .build()).get(0);

        OfficialActUpdateForm patch = new OfficialActUpdateForm();
        patch.setNotes("only notes changed");

        OfficialAct updated = service.update(created.getId(), patch);

        Object celebrant = updated.getMetadata().get("celebrant");
        assertThat(celebrant).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) celebrant).get("name")).isEqualTo("Rev. Mantido");
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
                        .actDate(new LocalDate(2024, 1, 1))
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
        LocalDate actDate = new LocalDate(2024, 2, 2);

        OfficialAct admission = service.create(
                OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE_E_BATISMO)
                        .personId(p.getId())
                        .actDate(actDate)
                        .build()).get(0);

        service.delete(admission.getId());

        Pessoa reloaded = pessoaService.findById(p.getId());
        // Dates carry historic meaning; only category is reverted.
        assertThat(reloaded.getDataBatismo()).isEqualTo(actDate.toDateTimeAtStartOfDay());
        assertThat(reloaded.getTipoBatismo()).isEqualTo(TipoBatismo.ADULTO);
        assertThat(reloaded.getDataProfissaoDeFe()).isEqualTo(actDate.toDateTimeAtStartOfDay());
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
                .actDate(new LocalDate(2024, 1, 1))
                .build());
        service.create(OfficialActFixture.builder(OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO)
                .personId(p.getId())
                .actDate(new LocalDate(2024, 2, 1))
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

    private OfficialActCreateForm baseForm(Pessoa pessoa, OfficialActFormEnum form) {
        OfficialActCreateForm createForm = new OfficialActCreateForm();
        createForm.setOfficialActFormId(form.getId());
        createForm.setActDate(new LocalDate(2024, 1, 1));
        createForm.setPersonIds(List.of(pessoa.getId()));
        createForm.setMetadata(new HashMap<>(OfficialActFixture.metadataMinimo(form)));
        return createForm;
    }
}
