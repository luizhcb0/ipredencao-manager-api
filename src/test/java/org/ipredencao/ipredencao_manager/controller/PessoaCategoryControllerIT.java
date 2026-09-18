package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PessoaCategoryControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/pessoas";

    @Autowired
    private PessoaService pessoaService;

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void updateCategory_updatesOnlyCategoryAndAuditFields() throws Exception {
        Pessoa pessoa = PessoaFixture.builder(pessoaService)
                .nome("João da Silva")
                .apelido("João")
                .email("joao@example.com")
                .telefone("61999990000")
                .sexo(Sexo.MASCULINO)
                .dataNascimento(new DateTime(1985, 1, 1, 0, 0))
                .estadoCivil(EstadoCivil.SOLTEIRO_SEM_RELACIONAMENTO)
                .categoria(CategoriaEnum.SOLICITAR_CARTA_TRANSFERENCIA)
                .campus("SEDE")
                .igrejaAnterior("Igreja de origem")
                .build();

        mockMvc.perform(patch(BASE + "/" + pessoa.getId() + "/categoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoriaId": 14}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pessoa.getId()))
                .andExpect(jsonPath("$.categoria.id").value(14));

        Pessoa updated = pessoaService.findById(pessoa.getId());
        assertThat(updated.getCategoria()).isEqualTo(CategoriaEnum.AGUARDANDO_CARTA_TRANSFERENCIA);
        assertThat(updated.getNome()).isEqualTo("João da Silva");
        assertThat(updated.getApelido()).isEqualTo("João");
        assertThat(updated.getEmail()).isEqualTo("joao@example.com");
        assertThat(updated.getTelefone()).isEqualTo("61999990000");
        assertThat(updated.getCampus()).isEqualTo("SEDE");
        assertThat(updated.getIgrejaAnterior()).isEqualTo("Igreja de origem");
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void updateCategory_returnsNotFoundForUnknownPerson() throws Exception {
        mockMvc.perform(patch(BASE + "/999999999/categoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoriaId": 14}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PRESBITERO")
    void updateCategory_returnsBadRequestForUnknownCategory() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(
                pessoaService, "Categoria Inválida", Sexo.FEMININO);

        mockMvc.perform(patch(BASE + "/" + pessoa.getId() + "/categoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoriaId": 999}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void updateCategory_appliesForDeacon() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Diácono Categoria", Sexo.MASCULINO);

        mockMvc.perform(patch(BASE + "/" + pessoa.getId() + "/categoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoriaId": 7}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(CategoriaEnum.MEMBRO_NAO_COMUNGANTE.getId()));
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void updatePerson_appliesCategoriaFromDeacon() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Diácono PUT", Sexo.FEMININO);

        mockMvc.perform(put(BASE + "/" + pessoa.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(personPutBody(pessoa, CategoriaEnum.MEMBRO_NAO_COMUNGANTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.id").value(CategoriaEnum.MEMBRO_NAO_COMUNGANTE.getId()));

        assertThat(pessoaService.findById(pessoa.getId()).getCategoria())
                .isEqualTo(CategoriaEnum.MEMBRO_NAO_COMUNGANTE);
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void updateCategory_returnsForbiddenForNonStaff() throws Exception {
        mockMvc.perform(patch(BASE + "/1/categoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoriaId": 14}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCategory_returnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(patch(BASE + "/1/categoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoriaId": 14}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private String personPutBody(Pessoa pessoa, CategoriaEnum categoria) throws Exception {
        var node = objectMapper.createObjectNode();
        node.put("id", pessoa.getId());
        node.put("nome", pessoa.getNome());
        node.put("sexo", pessoa.getSexo().name());
        node.put("dataNascimento", pessoa.getDataNascimento().toString());
        node.put("estadoCivil", pessoa.getEstadoCivil().name());
        node.put("categoria", categoria.getId());
        node.put("campus", pessoa.getCampus() != null ? pessoa.getCampus() : "SEDE");
        return objectMapper.writeValueAsString(node);
    }
}
