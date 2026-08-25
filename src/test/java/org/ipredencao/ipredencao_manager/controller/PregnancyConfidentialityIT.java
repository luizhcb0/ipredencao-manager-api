package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoRelacionamento;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.ipredencao.ipredencao_manager.service.EnderecoService;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Categoria 30 é linha invisível abaixo de presbítero. Vários casos usam a categoria 29
 * para impedir que o filtro pegue demais.
 */
@Transactional
class PregnancyConfidentialityIT extends IntegrationTestBase {

    private static final String PEOPLE = "/api/pessoas";
    private static final String CONFIDENTIAL = "GESTACAO_SIGILO_TEMPORARIO";

    @Autowired private PessoaService pessoaService;
    @Autowired private EnderecoService enderecoService;

    private record Family(Pessoa mother, Pessoa confidentialBaby, Pessoa openBaby) {}

    /** Mãe com dois bebês vinculados: um em sigilo, outro não. */
    private Family family(String suffix) {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae " + suffix, Sexo.FEMININO);
        Pessoa confidential = baby("Sigiloso " + suffix, CategoriaEnum.GESTACAO_SIGILO_TEMPORARIO, mother);
        Pessoa open = baby("Aberto " + suffix, CategoriaEnum.GESTACAO, mother);
        return new Family(mother, confidential, open);
    }

    private Pessoa baby(String nome, CategoriaEnum categoria, Pessoa mother) {
        Pessoa baby = PessoaFixture.builder(pessoaService)
                .nome(nome + " (bebê de " + mother.getNome() + ")")
                .sexo(Sexo.FEMININO)
                .dataNascimento(DateTime.now().plusMonths(3).withTimeAtStartOfDay())
                .estadoCivil(EstadoCivil.SOLTEIRO_SEM_RELACIONAMENTO)
                .categoria(categoria)
                .build();

        // Mesmo sentido da criação real: bebê → mãe.
        Relacionamento rel = new Relacionamento();
        rel.setPessoaRelacionadaId(mother.getId());
        rel.setTipoRelacionamento(TipoRelacionamento.MAE);
        asElder(() -> pessoaService.createRelationship(baby.getId(), rel));

        return baby;
    }

    /** Arranjo, não caso de teste: vincular a linha em sigilo é privilégio de presbítero. */
    private void asElder(Runnable arrange) {
        SecurityContext original = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(new SecurityContextImpl(
                    new UsernamePasswordAuthenticationToken("fixture", "n/a",
                            List.of(new SimpleGrantedAuthority("ROLE_PRESBITERO")))));
            arrange.run();
        } finally {
            SecurityContextHolder.setContext(original);
        }
    }

    /** {@code categorias} entra por id, não pelo nome do enum. */
    private String searchByCategory(String... extraEntries) {
        return "{\"categorias\":[" + CategoriaEnum.GESTACAO_SIGILO_TEMPORARIO.getId() + "]"
                + (extraEntries.length > 0 ? "," + String.join(",", extraEntries) : "")
                + "}";
    }

    // ===== busca por id =====

    @Test
    @WithMockUser(roles = "BOLETIM")
    void confidentialPerson_isNotFoundForBoletim() throws Exception {
        Family f = family("Boletim");

        mockMvc.perform(get(PEOPLE + "/{id}", f.confidentialBaby().getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(PEOPLE + "/{id}", f.openBaby().getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void confidentialPerson_isNotFoundForDeacon() throws Exception {
        Family f = family("Diacono");

        mockMvc.perform(get(PEOPLE + "/{id}", f.confidentialBaby().getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(PEOPLE + "/{id}", f.openBaby().getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void confidentialPerson_isVisibleForElder() throws Exception {
        Family f = family("Presbitero");

        mockMvc.perform(get(PEOPLE + "/{id}", f.confidentialBaby().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(30));
    }

    /** O corte por categoria só é seguro porque toda pessoa tem uma: "NULL <> 30" sumiria com a linha. */
    @Test
    void personWithoutCategory_isRejected() {
        Pessoa semCategoria = new Pessoa();
        semCategoria.setNome("Sem Categoria");
        semCategoria.setSexo(Sexo.FEMININO);

        assertThatThrownBy(() -> pessoaService.create(semCategoria))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("categoria");
    }

    // ===== busca paginada, relatório e resumo =====

    @Test
    @WithMockUser(roles = "DIACONO")
    void search_byConfidentialCategory_isEmptyForDeacon() throws Exception {
        family("Search");

        mockMvc.perform(post(PEOPLE + "/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchByCategory()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    /** A saída do filtro não é campo da query: pedi-la pelo corpo não muda nada. */
    @Test
    @WithMockUser(roles = "BOLETIM")
    void search_confidentialFlagFromBody_isIgnored() throws Exception {
        family("Flag");

        mockMvc.perform(post(PEOPLE + "/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchByCategory("\"includeConfidential\":true")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void search_byConfidentialCategory_returnsRowForElder() throws Exception {
        family("SearchElder");

        mockMvc.perform(post(PEOPLE + "/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchByCategory()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void generateReport_byConfidentialCategory_isEmptyForBoletim() throws Exception {
        family("Report");

        mockMvc.perform(post("/api/reports/generate-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchByCategory()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void summary_omitsConfidentialCategoryForBoletim() throws Exception {
        family("Summary");

        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personsByCategory." + CONFIDENTIAL).doesNotExist())
                .andExpect(jsonPath("$.personsByCategory.GESTACAO").exists());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void summary_countsConfidentialCategoryForElder() throws Exception {
        family("SummaryElder");

        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personsByCategory." + CONFIDENTIAL).exists());
    }

    // ===== relacionamentos da mãe =====

    @Test
    @WithMockUser(roles = "DIACONO")
    void motherRelationships_hideConfidentialBabyOnly() throws Exception {
        Family f = family("Rel");

        mockMvc.perform(get(PEOPLE + "/{id}", f.mother().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relacionamentos", hasSize(1)))
                .andExpect(jsonPath("$.relacionamentos[0].pessoaRelacionadaId")
                        .value(f.openBaby().getId().intValue()));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void motherRelationships_listBothForElder() throws Exception {
        Family f = family("RelElder");

        mockMvc.perform(get(PEOPLE + "/{id}", f.mother().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relacionamentos", hasSize(2)));
    }

    // ===== histórico =====

    @Test
    @WithMockUser(roles = "DIACONO")
    void history_ofConfidentialPerson_isNotFoundForDeacon() throws Exception {
        Family f = family("Hist");

        mockMvc.perform(get(PEOPLE + "/{id}/history", f.confidentialBaby().getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(PEOPLE + "/{id}/history", f.openBaby().getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void notes_ofConfidentialPerson_areNotFoundForDeacon() throws Exception {
        Family f = family("Notes");

        mockMvc.perform(get(PEOPLE + "/{id}/notes", f.confidentialBaby().getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(PEOPLE + "/{id}/notes", f.openBaby().getId()))
                .andExpect(status().isOk());
    }

    // ===== escrita =====

    @Test
    @WithMockUser(roles = "DIACONO")
    void deacon_cannotCreatePregnancyInSecrecy() throws Exception {
        mockMvc.perform(post(PEOPLE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pregnancyBody("Cria Sigilo", true)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void deacon_stillCreatesOpenPregnancy() throws Exception {
        mockMvc.perform(post(PEOPLE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pregnancyBody("Cria Aberta", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(29));
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void deacon_cannotEditConfidentialPregnancy() throws Exception {
        Family f = family("Edit");
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Edit", Sexo.MASCULINO);

        String body = objectMapper.writeValueAsString(Map.of(
                "motherId", f.mother().getId(),
                "fatherId", father.getId(),
                "expectedDueDate", DateTime.now().plusMonths(2).toString()
        ));

        mockMvc.perform(put(PEOPLE + "/{id}/pregnancy", f.confidentialBaby().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void deacon_cannotCloseConfidentialPregnancy() throws Exception {
        Family f = family("Close");

        mockMvc.perform(delete(PEOPLE + "/{id}/pregnancy", f.confidentialBaby().getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void deacon_cannotTurnOpenPregnancyIntoSecret() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Liga", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Liga", Sexo.MASCULINO);
        DateTime dueDate = DateTime.now().plusMonths(3);

        String created = mockMvc.perform(post(PEOPLE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "motherId", mother.getId(),
                                "fatherId", father.getId(),
                                "expectedDueDate", dueDate.toString(),
                                "confidential", false))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long pregnancyId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put(PEOPLE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "motherId", mother.getId(),
                                "fatherId", father.getId(),
                                "expectedDueDate", dueDate.toString(),
                                "confidential", true))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void deacon_completesOpenPregnancyCycle() throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae Ciclo", Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai Ciclo", Sexo.MASCULINO);
        DateTime dueDate = DateTime.now().plusMonths(3);

        String created = mockMvc.perform(post(PEOPLE + "/pregnancy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "motherId", mother.getId(),
                                "fatherId", father.getId(),
                                "expectedDueDate", dueDate.toString(),
                                "confidential", false))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long pregnancyId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(put(PEOPLE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "motherId", mother.getId(),
                                "fatherId", father.getId(),
                                "expectedDueDate", dueDate.plusDays(1).toString()))))
                .andExpect(status().isOk());

        mockMvc.perform(put(PEOPLE + "/{id}/pregnancy", pregnancyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Bebê Nascido",
                                "birthDate", DateTime.now().minusDays(1).toString(),
                                "gender", "MASCULINO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(16));
    }

    private String pregnancyBody(String suffix, boolean confidential) throws Exception {
        Pessoa mother = PessoaFixture.membroComungante(pessoaService, "Mae " + suffix, Sexo.FEMININO);
        Pessoa father = PessoaFixture.membroComungante(pessoaService, "Pai " + suffix, Sexo.MASCULINO);
        return objectMapper.writeValueAsString(Map.of(
                "motherId", mother.getId(),
                "fatherId", father.getId(),
                "expectedDueDate", DateTime.now().plusMonths(3).toString(),
                "confidential", confidential));
    }

    // ===== integridade do endereço =====

    /** A contagem que protege o endereço enxerga sigilo mesmo para quem não vê. */
    @Test
    @WithMockUser(roles = "DIACONO")
    void addressDelete_countsConfidentialResident() {
        Endereco endereco = new Endereco();
        endereco.setCep("50000-000");
        endereco.setLogradouro("Rua do Sigilo");
        endereco.setNumero("10");
        endereco.setBairro("Centro");
        endereco.setCidade("Recife");
        endereco.setEstado("PE");
        Endereco created = enderecoService.create(endereco);

        PessoaFixture.builder(pessoaService)
                .nome("Bebê Sigiloso no Endereço")
                .sexo(Sexo.FEMININO)
                .dataNascimento(DateTime.now().plusMonths(2).withTimeAtStartOfDay())
                .estadoCivil(EstadoCivil.SOLTEIRO_SEM_RELACIONAMENTO)
                .categoria(CategoriaEnum.GESTACAO_SIGILO_TEMPORARIO)
                .endereco(created)
                .build();

        assertThatThrownBy(() -> enderecoService.delete(created.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    /** A gestação herda o endereço do chefe: o id a mais denunciaria a linha. */
    @Test
    @WithMockUser(roles = "BOLETIM")
    void addressResidents_omitConfidentialForBoletim() throws Exception {
        Endereco shared = sharedAddress("Rua dos Moradores");
        Pessoa visivel = resident("Moradora Visivel", CategoriaEnum.MEMBRO_COMUNGANTE, shared);
        Pessoa sigiloso = resident("Bebê Sigiloso Morador", CategoriaEnum.GESTACAO_SIGILO_TEMPORARIO, shared);

        mockMvc.perform(get("/api/enderecos/{id}", shared.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoaIds", hasSize(1)))
                .andExpect(jsonPath("$.pessoaIds[0]").value(visivel.getId().intValue()))
                .andExpect(jsonPath("$.pessoaIds", not(hasItem(sigiloso.getId().intValue()))));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void addressResidents_listConfidentialForElder() throws Exception {
        Endereco shared = sharedAddress("Rua dos Moradores Elder");
        resident("Moradora Visivel Elder", CategoriaEnum.MEMBRO_COMUNGANTE, shared);
        resident("Bebê Sigiloso Elder", CategoriaEnum.GESTACAO_SIGILO_TEMPORARIO, shared);

        mockMvc.perform(get("/api/enderecos/{id}", shared.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoaIds", hasSize(2)));
    }

    /** O relatório escolhe categorias pelo corpo: pedir a 30 direto é o ataque óbvio. */
    @Test
    @WithMockUser(roles = "BOLETIM")
    void participationReport_byConfidentialCategory_isEmptyForBoletim() throws Exception {
        family("Participacao");

        mockMvc.perform(post("/api/serving-areas/participation-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participationBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void participationReport_byConfidentialCategory_returnsRowForElder() throws Exception {
        family("ParticipacaoElder");

        mockMvc.perform(post("/api/serving-areas/participation-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participationBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /** O POST é idempotente: sem o corte, devolveria o vínculo achado com o nome junto. */
    @Test
    @WithMockUser(roles = "DIACONO")
    void createRelationship_towardConfidentialPerson_isNotFound() throws Exception {
        Family f = family("NomeVinculo");

        Relacionamento rel = new Relacionamento();
        rel.setPessoaRelacionadaId(f.confidentialBaby().getId());
        rel.setTipoRelacionamento(TipoRelacionamento.FILHO);

        mockMvc.perform(post(PEOPLE + "/{id}/relacionamento", f.mother().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rel)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void createRelationship_onConfidentialPerson_isNotFound() throws Exception {
        Family f = family("VinculoNaMae");

        Relacionamento rel = new Relacionamento();
        rel.setPessoaRelacionadaId(f.mother().getId());
        rel.setTipoRelacionamento(TipoRelacionamento.MAE);

        mockMvc.perform(post(PEOPLE + "/{id}/relacionamento", f.confidentialBaby().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rel)))
                .andExpect(status().isNotFound());
    }

    private String participationBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "categoryIds", List.of(CategoriaEnum.GESTACAO_SIGILO_TEMPORARIO.getId()),
                "status", "ALL"));
    }

    private Endereco sharedAddress(String logradouro) {
        Endereco endereco = new Endereco();
        endereco.setCep("50000-000");
        endereco.setLogradouro(logradouro);
        endereco.setNumero("20");
        endereco.setBairro("Centro");
        endereco.setCidade("Recife");
        endereco.setEstado("PE");
        return enderecoService.create(endereco);
    }

    private Pessoa resident(String nome, CategoriaEnum categoria, Endereco endereco) {
        return PessoaFixture.builder(pessoaService)
                .nome(nome)
                .sexo(Sexo.FEMININO)
                .estadoCivil(EstadoCivil.SOLTEIRO_SEM_RELACIONAMENTO)
                .categoria(categoria)
                .endereco(endereco)
                .build();
    }
}
