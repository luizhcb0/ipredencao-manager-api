package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaInclude;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoRelacionamento;
import org.ipredencao.ipredencao_manager.repository.OfficialActRepository;
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

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PregnancyControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/pessoas";

    @Autowired
    private PessoaService pessoaService;

    @Autowired
    private OfficialActRepository officialActRepository;

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void createPregnancy_withBabyName_buildsLegacyNameAndNickname() throws Exception {
        Pessoa familyHead = PessoaFixture.builder(pessoaService)
                .nome("João Chefe")
                .sexo(Sexo.MASCULINO)
                .dataNascimento(new DateTime(1980, 1, 1, 0, 0))
                .estadoCivil(org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil.SOLTEIRO_SEM_RELACIONAMENTO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
                .campus("SEDE")
                .build();
        familyHead.setChefeDeFamiliaId(familyHead.getId());
        familyHead = pessoaService.update(familyHead);

        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Maria Silva", Sexo.FEMININO);
        mother.setCampus("SEDE");
        mother.setChefeDeFamiliaId(familyHead.getId());
        mother = pessoaService.update(mother);

        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pedro Silva", Sexo.MASCULINO);

        DateTime expectedDueDate = DateTime.now().plusMonths(3).withTimeAtStartOfDay();

        String body = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "name", "Ana",
                "gender", "FEMININO",
                "confidential", false
        ));

        mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(29))
                .andExpect(jsonPath("$.nome").value("Ana (bebê de Maria Silva e Pedro Silva)"))
                .andExpect(jsonPath("$.apelido").value("(bebê de Maria Silva e Pedro Silva)"))
                .andExpect(jsonPath("$.chefeDeFamilia.id").value(familyHead.getId()));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void createPregnancy_pastDueDate_returns400() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Teste", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Teste", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().minusDays(1);

        String body = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "confidential", false
        ));

        mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void createPregnancy_confidential_usesCategory30() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Sigilo", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Sigilo", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().plusMonths(2);

        String body = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "confidential", true
        ));

        mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(30));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void motherListsChildAfterCreatePregnancy() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Camila Biagi", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Camila", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().plusMonths(4);

        String body = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "confidential", false
        ));

        mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        Pessoa motherReloaded = pessoaService.find(PessoaQuery.builder()
                .id(mother.getId())
                .includes(PessoaInclude.RELACIONAMENTOS)
                .build()).getFirst();

        assertThat(motherReloaded.getRelacionamentos()).anyMatch(r ->
                r.getTipoRelacionamento() == TipoRelacionamento.FILHO);
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void registerBirth_convertsToCategory16() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Nascimento", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Nascimento", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().plusMonths(2);

        String createBody = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "name", "Bebê",
                "confidential", false
        ));

        String createResponse = mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long pregnancyId = objectMapper.readTree(createResponse).get("id").asLong();
        DateTime birthDate = DateTime.now().minusDays(1).withTimeAtStartOfDay();

        String birthBody = objectMapper.writeValueAsString(Map.of(
                "name", "Manuela Silva",
                "birthDate", birthDate.toString(),
                "gender", "FEMININO"
        ));

        mockMvc.perform(put(BASE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(birthBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(16))
                .andExpect(jsonPath("$.nome").value("Manuela Silva"));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void createPregnancy_propagatesFamilyHeadAddress() throws Exception {
        Pessoa familyHead = PessoaFixture.builder(pessoaService)
                .nome("Chefe Endereco")
                .sexo(Sexo.MASCULINO)
                .dataNascimento(new DateTime(1980, 1, 1, 0, 0))
                .estadoCivil(org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil.SOLTEIRO_SEM_RELACIONAMENTO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
                .enderecoPadrao()
                .build();
        familyHead.setChefeDeFamiliaId(familyHead.getId());
        familyHead = pessoaService.update(familyHead);

        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Endereco", Sexo.FEMININO);
        mother.setChefeDeFamiliaId(familyHead.getId());
        mother = pessoaService.update(mother);

        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Endereco", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().plusMonths(2);

        String createBody = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "confidential", false
        ));

        mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco.logradouro").value("Rua Teste"))
                .andExpect(jsonPath("$.endereco.numero").value("100"));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void closePregnancy_returns204() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Encerrar", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Encerrar", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().plusMonths(1);

        String createBody = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "confidential", false
        ));

        String createResponse = mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long pregnancyId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete(BASE + "/{id}/pregnancy", pregnancyId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/{id}", pregnancyId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void closeRegularMember_returns400() throws Exception {
        Pessoa member = PessoaFixture.membroComungante(pessoaService, "Membro", Sexo.MASCULINO);

        mockMvc.perform(delete(BASE + "/{id}/pregnancy", member.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void closePregnancyWithOfficialAct_returns409() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Ato", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Ato", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().plusMonths(1);

        String createBody = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "confidential", false
        ));

        String createResponse = mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long pregnancyId = objectMapper.readTree(createResponse).get("id").asLong();

        OfficialAct act = new OfficialAct();
        act.setOfficialActFormId(OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA.getId());
        act.setPersonId(pregnancyId);
        act.setActDate(org.joda.time.LocalDate.now());
        act.setMinuteNumber("999");
        act.setMetadata(new HashMap<>(OfficialActFixture.metadataMinimo(OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA)));
        officialActRepository.insert(act);

        mockMvc.perform(delete(BASE + "/{id}/pregnancy", pregnancyId))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void updatePregnancy_changesFather() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Update", Sexo.FEMININO);
        Pessoa father1 = PessoaFixture.membroComungante(pessoaService, "Pai Um", Sexo.MASCULINO);
        Pessoa father2 = PessoaFixture.membroComungante(pessoaService, "Pai Dois", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().plusMonths(5);

        String createBody = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father1.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "confidential", false
        ));

        String createResponse = mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long pregnancyId = objectMapper.readTree(createResponse).get("id").asLong();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father2.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "confidential", false
        ));

        mockMvc.perform(put(BASE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apelido").value("(bebê de Mae Update e Pai Dois)"));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void updatePregnancy_withoutConfidentialField_preservesCategory() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Preserve", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Preserve", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().plusMonths(5);

        String createBody = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "confidential", true
        ));

        String createResponse = mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(30))
                .andReturn().getResponse().getContentAsString();

        Long pregnancyId = objectMapper.readTree(createResponse).get("id").asLong();
        String updateBody = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.plusDays(1).toString()
        ));

        mockMvc.perform(put(BASE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(30));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void registerBirth_futureBirthDate_returns400() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Futuro", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Futuro", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().plusMonths(2);

        String createBody = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "confidential", false
        ));

        String createResponse = mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long pregnancyId = objectMapper.readTree(createResponse).get("id").asLong();
        String birthBody = objectMapper.writeValueAsString(Map.of(
                "name", "Bebe Futuro",
                "birthDate", DateTime.now().plusDays(1).toString(),
                "gender", "MASCULINO"
        ));

        mockMvc.perform(put(BASE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(birthBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void registerBirth_preservesParentRelationships() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Rel", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Rel", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().plusMonths(2);

        String createBody = objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", expectedDueDate.toString(),
                "confidential", false
        ));

        String createResponse = mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long pregnancyId = objectMapper.readTree(createResponse).get("id").asLong();
        String birthBody = objectMapper.writeValueAsString(Map.of(
                "name", "Bebe Rel",
                "birthDate", DateTime.now().minusDays(1).toString(),
                "gender", "MASCULINO"
        ));

        mockMvc.perform(put(BASE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(birthBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(16));

        Pessoa reloaded = pessoaService.find(PessoaQuery.builder()
                .id(pregnancyId)
                .includes(PessoaInclude.RELACIONAMENTOS)
                .build()).getFirst();

        assertThat(reloaded.getRelacionamentos()).anyMatch(r ->
                r.getTipoRelacionamento() == TipoRelacionamento.MAE
                        && r.getPessoaRelacionadaId().equals(mother.getId()));
        assertThat(reloaded.getRelacionamentos()).anyMatch(r ->
                r.getTipoRelacionamento() == TipoRelacionamento.PAI
                        && r.getPessoaRelacionadaId().equals(father.getId()));
    }
}
