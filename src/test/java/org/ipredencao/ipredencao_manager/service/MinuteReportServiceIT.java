package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.controller.form.OfficialActCreateForm;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActTypeEnum;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.CategorySection;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.FormGroup;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.MinuteReportLine;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.MinuteReportResponse;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.TypeGroup;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.OfficialActFixture;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link MinuteReportService}. Covers input validation, category
 * ordering, type/form grouping, and line content rendered via {@link MinuteReportFormatter}.
 */
@Transactional
class MinuteReportServiceIT extends IntegrationTestBase {

    @Autowired private MinuteReportService service;
    @Autowired private OfficialActService officialActService;
    @Autowired private PessoaService pessoaService;

    // ===== validation =====

    @Test
    void generate_throwsWhenMinuteNumberIsNull() {
        assertThatThrownBy(() -> service.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minuteNumber");
    }

    @Test
    void generate_throwsWhenMinuteNumberIsBlank() {
        assertThatThrownBy(() -> service.generate("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minuteNumber");
    }

    @Test
    void generate_throwsWhenNoActsExistForMinute() {
        assertThatThrownBy(() -> service.generate("nonexistent-minute-xyz"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ===== header =====

    @Test
    void generate_returnsHeaderWithMinuteNumberAndDate() {
        String minute = uniqueMinute("HDR");
        DateTime minuteDate = new DateTime(2024, 3, 10, 0, 0);
        createAct(OfficialActFormEnum.ADM_MC_PROFISSAO_FE,
                somePessoa("Header"),
                new DateTime(2024, 3, 5, 0, 0),
                minute, minuteDate);

        MinuteReportResponse report = service.generate(minute);

        assertThat(report.getMinuteNumber()).isEqualTo(minute);
        assertThat(report.getMinuteDate()).isEqualTo(minuteDate);
    }

    // ===== ordering =====

    @Test
    void generate_categorySectionsRenderInCanonicalOrder() {
        // Inserted in reverse order; the service must still emit ADMISSAO → DEMISSAO.
        String minute = uniqueMinute("ORD");
        DateTime date = new DateTime(2024, 4, 1, 0, 0);
        createAct(OfficialActFormEnum.DEM_MC_EXCLUSAO_A_PEDIDO, somePessoa("Dem"), date, minute, date);
        createAct(OfficialActFormEnum.ADM_MC_PROFISSAO_FE,      somePessoa("Adm"), date, minute, date);

        MinuteReportResponse report = service.generate(minute);

        assertThat(report.getSections())
                .extracting(CategorySection::getCategory)
                .containsExactly("ADMISSAO", "DEMISSAO");

        // Titles must match the human-readable mapping.
        assertThat(report.getSections())
                .extracting(CategorySection::getTitle)
                .containsExactly("Admissões", "Demissões");
    }

    @Test
    void generate_skipsCategoriesWithNoActs() {
        String minute = uniqueMinute("SKIP");
        DateTime date = new DateTime(2024, 5, 1, 0, 0);
        createAct(OfficialActFormEnum.ADM_MC_PROFISSAO_FE, somePessoa("Only adm"), date, minute, date);

        MinuteReportResponse report = service.generate(minute);

        assertThat(report.getSections())
                .extracting(CategorySection::getCategory)
                .containsExactly("ADMISSAO");
    }

    // ===== grouping =====

    @Test
    void generate_groupsByTypeWithinCategory() {
        // Two admission types in the same minute: MC (type id 1) and MNC (type id 2).
        String minute = uniqueMinute("TYPE");
        DateTime date = new DateTime(2024, 6, 1, 0, 0);
        createAct(OfficialActFormEnum.ADM_MC_PROFISSAO_FE,     somePessoa("Mc 1"),  date, minute, date);
        createAct(OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA, somePessoaInfantil("Mnc 1"), date, minute, date);

        MinuteReportResponse report = service.generate(minute);

        CategorySection admissao = report.getSections().get(0);
        assertThat(admissao.getTypes())
                .extracting(TypeGroup::getTypeId)
                .containsExactlyInAnyOrder(
                        OfficialActTypeEnum.ADMISSAO_MEMBRO_COMUNGANTE.getId(),
                        OfficialActTypeEnum.ADMISSAO_MEMBRO_NAO_COMUNGANTE.getId());
    }

    @Test
    void generate_groupsLinesByFormWithinType() {
        // Two MC admission forms: ADM_MC_PROFISSAO_FE (id 1) and ADM_MC_CARTA_TRANSFERENCIA (id 3).
        String minute = uniqueMinute("FORM");
        DateTime date = new DateTime(2024, 7, 1, 0, 0);
        createAct(OfficialActFormEnum.ADM_MC_PROFISSAO_FE,       somePessoa("Form A"), date, minute, date);
        createAct(OfficialActFormEnum.ADM_MC_PROFISSAO_FE,       somePessoa("Form B"), date, minute, date);
        createAct(OfficialActFormEnum.ADM_MC_CARTA_TRANSFERENCIA, somePessoa("Form C"), date, minute, date);

        MinuteReportResponse report = service.generate(minute);

        TypeGroup mc = report.getSections().get(0).getTypes().stream()
                .filter(t -> t.getTypeId().equals(OfficialActTypeEnum.ADMISSAO_MEMBRO_COMUNGANTE.getId()))
                .findFirst().orElseThrow();

        assertThat(mc.getForms())
                .extracting(FormGroup::getFormId)
                .containsExactlyInAnyOrder(
                        OfficialActFormEnum.ADM_MC_PROFISSAO_FE.getId(),
                        OfficialActFormEnum.ADM_MC_CARTA_TRANSFERENCIA.getId());

        FormGroup profissao = formById(mc, OfficialActFormEnum.ADM_MC_PROFISSAO_FE.getId());
        assertThat(profissao.getLines()).hasSize(2);

        FormGroup carta = formById(mc, OfficialActFormEnum.ADM_MC_CARTA_TRANSFERENCIA.getId());
        assertThat(carta.getLines()).hasSize(1);

        // Form group carries the metadata schema from OfficialActTypesRepository.
        assertThat(profissao.getFormName()).isNotBlank();
        assertThat(profissao.getArticleClause()).isEqualTo(OfficialActFormEnum.ADM_MC_PROFISSAO_FE.getArticleClause());
    }

    // ===== line content =====

    @Test
    void generate_linesCarryActIdPersonIdAndAdmissionOrderNumber() {
        String minute = uniqueMinute("LINE");
        DateTime date = new DateTime(2024, 8, 1, 0, 0);
        Pessoa pessoa = somePessoa("Carry");
        OfficialAct admission = createAct(OfficialActFormEnum.ADM_MC_PROFISSAO_FE, pessoa, date, minute, date);

        MinuteReportResponse report = service.generate(minute);

        MinuteReportLine line = report.getSections().get(0).getTypes().get(0).getForms().get(0).getLines().get(0);
        assertThat(line.getActId()).isEqualTo(admission.getId());
        assertThat(line.getPersonId()).isEqualTo(pessoa.getId());
        assertThat(line.getAdmissionOrderNumber())
                .as("admission consumes a admission_order_number and the line surfaces it")
                .isEqualTo(admission.getAdmissionOrderNumber());
    }

    @Test
    void generate_lineTextContainsRenderedFragmentsFromFormatter() {
        String minute = uniqueMinute("TXT");
        DateTime actDate = new DateTime(2024, 9, 15, 0, 0);
        Pessoa pessoa = somePessoa("João da Silva");
        OfficialAct admission = createAct(OfficialActFormEnum.ADM_MC_PROFISSAO_FE, pessoa, actDate, minute, actDate);

        MinuteReportResponse report = service.generate(minute);

        String text = report.getSections().get(0).getTypes().get(0).getForms().get(0).getLines().get(0).getText();
        // Spot-check the formatter wiring (full formatter coverage belongs to its own tests).
        assertThat(text)
                .contains("15/09/2024")
                .contains("JOÃO DA SILVA")
                .contains("(" + admission.getAdmissionOrderNumber() + ")")
                .contains("profissão de fé");
    }

    @Test
    void generate_ignoresActsFromOtherMinutes() {
        String mine  = uniqueMinute("OWN");
        String other = uniqueMinute("OTH");
        DateTime date = new DateTime(2024, 10, 1, 0, 0);
        Pessoa pessoaA = somePessoa("Own");
        Pessoa pessoaB = somePessoa("Other");
        createAct(OfficialActFormEnum.ADM_MC_PROFISSAO_FE, pessoaA, date, mine,  date);
        createAct(OfficialActFormEnum.ADM_MC_PROFISSAO_FE, pessoaB, date, other, date);

        MinuteReportResponse report = service.generate(mine);

        List<MinuteReportLine> lines = report.getSections().get(0).getTypes().get(0).getForms().get(0).getLines();
        assertThat(lines)
                .extracting(MinuteReportLine::getPersonId)
                .containsExactly(pessoaA.getId());
    }

    // ===== helpers =====

    private Pessoa somePessoa(String nome) {
        return PessoaFixture.membroComungante(pessoaService, nome, Sexo.MASCULINO);
    }

    private Pessoa somePessoaInfantil(String nome) {
        return PessoaFixture.menorNaoComungante(
                pessoaService, nome, Sexo.FEMININO, new DateTime(2020, 5, 5, 0, 0));
    }

    private OfficialAct createAct(OfficialActFormEnum form, Pessoa pessoa, DateTime actDate,
                                  String minuteNumber, DateTime minuteDate) {
        OfficialActCreateForm createForm = OfficialActFixture.builder(form)
                .personId(pessoa.getId())
                .actDate(actDate)
                .minuteNumber(minuteNumber)
                .minuteDate(minuteDate)
                .build();
        return officialActService.create(createForm).get(0);
    }

    private static FormGroup formById(TypeGroup type, Long formId) {
        return type.getForms().stream()
                .filter(f -> f.getFormId().equals(formId))
                .findFirst().orElseThrow();
    }

    /** Minute numbers are global; uniqueness avoids collisions when transactions leak. */
    private static String uniqueMinute(String tag) {
        return "MR-IT-" + tag + "-" + System.nanoTime();
    }
}
