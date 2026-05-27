package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.controller.form.OfficialActCreateForm;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActQuery;
import org.ipredencao.ipredencao_manager.controller.form.OfficialActUpdateForm;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.service.OfficialActService;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.OfficialActFixture;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc integration tests for {@link OfficialActController}. Verifies the security
 * matrix (PRESBITERO/ADMIN allowed; others denied), client-side sanitization of the
 * backfill-only flags, the {@link org.ipredencao.ipredencao_manager.config.GlobalExceptionHandler}
 * wiring (400/404), and round-trips for every endpoint.
 */
@Transactional
class OfficialActControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/atos-oficiais";

    @Autowired private OfficialActService officialActService;
    @Autowired private PessoaService pessoaService;

    // ===== security matrix =====

    @Test
    void create_returnsUnauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void create_returnsForbiddenForDiacono() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void findById_returnsForbiddenForBoletim() throws Exception {
        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void catalog_isAccessibleByPresbitero() throws Exception {
        mockMvc.perform(get(BASE + "/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(4)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void catalog_isAccessibleByAdmin() throws Exception {
        mockMvc.perform(get(BASE + "/catalogo"))
                .andExpect(status().isOk());
    }

    // ===== POST /api/atos-oficiais =====

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void create_returnsOkAndPersistsActsForEachPerson() throws Exception {
        Pessoa a = somePessoa("Create A");
        Pessoa b = somePessoa("Create B");

        OfficialActCreateForm dto = OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personIds(List.of(a.getId(), b.getId()))
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .build();

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].personId").value(a.getId()))
                .andExpect(jsonPath("$[1].personId").value(b.getId()))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].officialActFormId").value(OfficialActFormEnum.ADM_MC_PROFISSAO_FE.getId()));
    }

    /**
     * Defense-in-depth: even if a client sends skip flags in the JSON, the controller
     * forces them to {@code false}. The proof is observable: numero_ordem_admissao is
     * assigned and the pessoa's categoria is mutated.
     */
    @Test
    @WithMockUser(roles = "PRESBITERO")
    void create_sanitizesSkipFlagsFromClientPayload() throws Exception {
        Pessoa p = somePessoaInfantil("Skip flags");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("officialActFormId", OfficialActFormEnum.ADM_MC_PROFISSAO_FE.getId());
        body.put("actDate", "2024-01-01T00:00:00.000-03:00");
        body.put("personIds", List.of(p.getId()));
        body.put("metadata", Map.of("celebrante", Map.of("name", "Rev. Teste")));
        body.put("skipEffects", true);
        body.put("skipNumeroOrdemAdmissao", true);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroOrdemAdmissao").isNumber()); // assigned despite skip=true

        Pessoa reloaded = pessoaService.findById(p.getId());
        assertThat(reloaded.getCategoria())
                .as("effects must run despite skipEffects=true in payload")
                .isEqualTo(CategoriaEnum.MEMBRO_COMUNGANTE);
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void create_returns400WhenFormIdIsUnknown() throws Exception {
        Pessoa p = somePessoa("Bad form");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("officialActFormId", 99999);
        body.put("actDate", "2024-01-01T00:00:00.000-03:00");
        body.put("personIds", List.of(p.getId()));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ===== GET /{id} =====

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void findById_returnsOk() throws Exception {
        OfficialAct act = createOneAct(somePessoa("Find"));

        mockMvc.perform(get(BASE + "/" + act.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(act.getId()))
                .andExpect(jsonPath("$.personId").value(act.getPersonId()));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void findById_returns404WhenUnknown() throws Exception {
        mockMvc.perform(get(BASE + "/999999"))
                .andExpect(status().isNotFound());
    }

    // ===== PUT /{id} =====

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void update_returnsOkAndModifiesMutableFields() throws Exception {
        OfficialAct act = createOneAct(somePessoa("Update"));

        OfficialActUpdateForm patch = new OfficialActUpdateForm();
        patch.setMinuteNumber("ATA-777");
        patch.setMinuteDate(new DateTime(2024, 12, 1, 0, 0));
        patch.setNotes("updated via http");
        patch.setMetadata(java.util.Map.of("celebrante", java.util.Map.of("id", 7, "name", "Rev. HTTP")));

        mockMvc.perform(put(BASE + "/" + act.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(act.getId()))
                .andExpect(jsonPath("$.minuteNumber").value("ATA-777"))
                .andExpect(jsonPath("$.notes").value("updated via http"))
                .andExpect(jsonPath("$.metadata.celebrante.name").value("Rev. HTTP"))
                .andExpect(jsonPath("$.metadata.celebrante.id").value(7))
                // immutable fields stay put
                .andExpect(jsonPath("$.officialActFormId").value(act.getOfficialActFormId()))
                .andExpect(jsonPath("$.personId").value(act.getPersonId()));
    }

    // ===== DELETE /{id} =====

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void delete_returnsNoContent() throws Exception {
        OfficialAct act = createOneAct(somePessoa("Delete"));

        mockMvc.perform(delete(BASE + "/" + act.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/" + act.getId()))
                .andExpect(status().isNotFound());
    }

    // ===== POST /search =====

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void search_returnsPagedResponseFilteringByPerson() throws Exception {
        Pessoa p = somePessoa("Search");
        createOneAct(p);
        createOneAct(p);

        OfficialActQuery query = new OfficialActQuery();
        query.setPersonId(p.getId());

        mockMvc.perform(post(BASE + "/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(query)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.page.total").value(2));
    }

    // ===== GET /atas/{minuteNumber} =====

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void findByMinuteNumber_returnsListForExistingAta() throws Exception {
        String minute = "MR-CTRL-" + System.nanoTime();
        Pessoa p = somePessoa("By minute");
        createOneActWithMinute(p, minute);

        mockMvc.perform(get(BASE + "/atas/" + minute))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].minuteNumber").value(minute));
    }

    // ===== GET /atas/{minuteNumber}/relatorio =====

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void minuteReport_returnsStructuredResponse() throws Exception {
        String minute = "MR-RPT-" + System.nanoTime();
        Pessoa p = somePessoa("Relatorio");
        createOneActWithMinute(p, minute);

        mockMvc.perform(get(BASE + "/atas/" + minute + "/relatorio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minuteNumber").value(minute))
                .andExpect(jsonPath("$.sections").isArray())
                .andExpect(jsonPath("$.sections[0].category").value("ADMISSAO"))
                .andExpect(jsonPath("$.sections[0].types[0].forms[0].lines[0].personId").value(p.getId()));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void minuteReport_returns404WhenNoActs() throws Exception {
        mockMvc.perform(get(BASE + "/atas/no-such-minute-xyz/relatorio"))
                .andExpect(status().isNotFound());
    }

    // ===== GET /catalogo =====

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void catalog_returnsAllFourTypes() throws Exception {
        mockMvc.perform(get(BASE + "/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(4)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].forms").isArray());
    }

    // ===== helpers =====

    private Pessoa somePessoa(String nome) {
        return PessoaFixture.membroComungante(pessoaService, nome, Sexo.MASCULINO);
    }

    private Pessoa somePessoaInfantil(String nome) {
        return PessoaFixture.menorNaoComungante(
                pessoaService, nome, Sexo.FEMININO, new DateTime(2020, 1, 1, 0, 0));
    }

    private OfficialAct createOneAct(Pessoa pessoa) {
        return officialActService.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(pessoa.getId())
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .build()).get(0);
    }

    private OfficialAct createOneActWithMinute(Pessoa pessoa, String minute) {
        return officialActService.create(OfficialActFixture.builder(OfficialActFormEnum.ADM_MC_PROFISSAO_FE)
                .personId(pessoa.getId())
                .actDate(new DateTime(2024, 1, 1, 0, 0))
                .minuteNumber(minute)
                .minuteDate(new DateTime(2024, 1, 5, 0, 0))
                .build()).get(0);
    }
}
