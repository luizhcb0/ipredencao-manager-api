package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.controller.form.PregnancyCreateForm;
import org.ipredencao.ipredencao_manager.controller.form.PregnancyUpdateForm;
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
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

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

        PregnancyCreateForm form = pregnancy(
                mother, father, DateTime.now().plusMonths(3).withTimeAtStartOfDay());
        form.setName("Ana");
        form.setGender(Sexo.FEMININO);

        mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
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

        mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                pregnancy(mother, father, DateTime.now().minusDays(1)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void createPregnancy_confidential_usesCategory30() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Sigilo", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Sigilo", Sexo.MASCULINO);

        PregnancyCreateForm form = pregnancy(mother, father, DateTime.now().plusMonths(2));
        form.setConfidential(true);

        mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(30));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void motherListsChildAfterCreatePregnancy() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Camila Biagi", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Camila", Sexo.MASCULINO);

        mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                pregnancy(mother, father, DateTime.now().plusMonths(4)))))
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

        PregnancyCreateForm create = pregnancy(mother, father, DateTime.now().plusMonths(2));
        create.setName("Bebê");
        Long pregnancyId = postedPregnancyId(create);

        PregnancyUpdateForm birth = birth(
                "Manuela Silva",
                DateTime.now().minusDays(1).withTimeAtStartOfDay(),
                Sexo.FEMININO);

        mockMvc.perform(put(BASE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(birth)))
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

        mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                pregnancy(mother, father, DateTime.now().plusMonths(2)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endereco.logradouro").value("Rua Teste"))
                .andExpect(jsonPath("$.endereco.numero").value("100"));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void closePregnancy_returns204() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Encerrar", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Encerrar", Sexo.MASCULINO);
        Long pregnancyId = postedPregnancyId(
                pregnancy(mother, father, DateTime.now().plusMonths(1)));

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
        Long pregnancyId = postedPregnancyId(
                pregnancy(mother, father, DateTime.now().plusMonths(1)));

        // Insert direto: o service do ato mudaria a categoria e o close viraria 400, não 409.
        OfficialActFormEnum form = OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA;
        OfficialAct act = new OfficialAct();
        act.setOfficialActFormId(form.getId());
        act.setPersonId(pregnancyId);
        act.setActDate(LocalDate.now());
        act.setMetadata(new HashMap<>(OfficialActFixture.metadataMinimo(form)));
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
        Long pregnancyId = postedPregnancyId(pregnancy(mother, father1, expectedDueDate));

        PregnancyUpdateForm update = new PregnancyUpdateForm();
        update.setMotherId(mother.getId());
        update.setFatherId(father2.getId());
        update.setExpectedDueDate(expectedDueDate);
        update.setConfidential(false);

        mockMvc.perform(put(BASE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apelido").value("(bebê de Mae Update e Pai Dois)"));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void updatePregnancy_withoutConfidentialField_preservesCategory() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Preserve", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Preserve", Sexo.MASCULINO);
        DateTime expectedDueDate = DateTime.now().plusMonths(5);

        PregnancyCreateForm create = pregnancy(mother, father, expectedDueDate);
        create.setConfidential(true);
        Long pregnancyId = postedPregnancyId(create);

        mockMvc.perform(get(BASE + "/{id}", pregnancyId))
                .andExpect(jsonPath("$.categoria.id").value(30));

        PregnancyUpdateForm update = new PregnancyUpdateForm();
        update.setMotherId(mother.getId());
        update.setFatherId(father.getId());
        update.setExpectedDueDate(expectedDueDate.plusDays(1));

        mockMvc.perform(put(BASE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(30));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void registerBirth_futureBirthDate_returns400() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Futuro", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Futuro", Sexo.MASCULINO);
        Long pregnancyId = postedPregnancyId(
                pregnancy(mother, father, DateTime.now().plusMonths(2)));

        mockMvc.perform(put(BASE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                birth("Bebe Futuro", DateTime.now().plusDays(1), Sexo.MASCULINO))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void registerBirth_preservesParentRelationships() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Rel", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Rel", Sexo.MASCULINO);
        Long pregnancyId = postedPregnancyId(
                pregnancy(mother, father, DateTime.now().plusMonths(2)));

        mockMvc.perform(put(BASE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                birth("Bebe Rel", DateTime.now().minusDays(1), Sexo.MASCULINO))))
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

    private static PregnancyCreateForm pregnancy(Pessoa mother, Pessoa father, DateTime dueDate) {
        PregnancyCreateForm form = new PregnancyCreateForm();
        form.setMotherId(mother.getId());
        form.setFatherId(father.getId());
        form.setExpectedDueDate(dueDate);
        return form;
    }

    private static PregnancyUpdateForm birth(String name, DateTime birthDate, Sexo gender) {
        PregnancyUpdateForm form = new PregnancyUpdateForm();
        form.setName(name);
        form.setBirthDate(birthDate);
        form.setGender(gender);
        return form;
    }

    private Long postedPregnancyId(PregnancyCreateForm form) throws Exception {
        String json = mockMvc.perform(post(BASE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(form)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }
}
