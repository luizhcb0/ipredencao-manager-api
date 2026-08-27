package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
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

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorte do BOLETIM: consulta pessoas, categorias e relatórios; formulários saem.
 * Os casos positivos existem para um aperto de matcher não derrubá-los em silêncio.
 */
@Transactional
class BoletimAccessIT extends IntegrationTestBase {

    private static final String FORMS = "/api/formulario-pessoa";
    private static final String PEOPLE = "/api/pessoas";

    @Autowired private PessoaService pessoaService;

    // ===== formulários: DIACONO+ =====

    @Test
    @WithMockUser(roles = "BOLETIM")
    void formsSearch_isForbiddenForBoletim() throws Exception {
        mockMvc.perform(post(FORMS + "/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void formById_isForbiddenForBoletim() throws Exception {
        mockMvc.perform(get(FORMS + "/{id}", 1))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void formsSearch_isAllowedForDeacon() throws Exception {
        mockMvc.perform(post(FORMS + "/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void formById_isAllowedForDeacon() throws Exception {
        String created = mockMvc.perform(post(FORMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publicFormBody("Formulário Teste")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get(FORMS + "/{id}", id))
                .andExpect(status().isOk());
    }

    /** A captação pública não pode ter sido fechada junto com a leitura. */
    @Test
    void formCreate_staysPublic() throws Exception {
        mockMvc.perform(post(FORMS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publicFormBody("Captação Pública")))
                .andExpect(status().isOk());
    }

    /** Só os campos NOT NULL da tabela. */
    private String publicFormBody(String nome) throws Exception {
        FormularioPessoa form = new FormularioPessoa();
        form.setNome(nome);
        form.setSexo(Sexo.FEMININO);
        form.setEmail("captacao@exemplo.com");
        form.setTelefone("81999990000");
        form.setCampus("SEDE");
        form.setDataNascimento(DateTime.now().minusYears(30).withTimeAtStartOfDay());
        form.setCpf("00000000000");
        form.setRg("0000000");
        form.setEnderecoCep("50000-000");
        form.setEnderecoLogradouro("Rua da Captação");
        form.setEnderecoNumero("100");
        return objectMapper.writeValueAsString(form);
    }

    // ===== consulta liberada ao BOLETIM =====

    @Test
    @WithMockUser(roles = "BOLETIM")
    void personById_isAllowedForBoletim() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Pessoa Boletim", Sexo.FEMININO);

        mockMvc.perform(get(PEOPLE + "/{id}", pessoa.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void peopleSearch_isAllowedForBoletim() throws Exception {
        mockMvc.perform(post(PEOPLE + "/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    /** A busca ignora o filtro; o campo continua no payload. */
    @Test
    @WithMockUser(roles = "BOLETIM")
    void personById_includesBookmarkForBoletim() throws Exception {
        Pessoa pessoa = PessoaFixture.builder(pessoaService)
                .nome("Com Pendencia Boletim")
                .sexo(Sexo.FEMININO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
                .bookmark(true)
                .build();

        mockMvc.perform(get(PEOPLE + "/{id}", pessoa.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmark").value(true));
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void personById_includesBookmarkForDeacon() throws Exception {
        Pessoa pessoa = PessoaFixture.builder(pessoaService)
                .nome("Com Pendencia Diacono")
                .sexo(Sexo.FEMININO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
                .bookmark(true)
                .build();

        mockMvc.perform(get(PEOPLE + "/{id}", pessoa.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmark").value(true));
    }

    /** Pedir a flag no corpo não filtra: o boletim veria só quem tem pendência. */
    @Test
    @WithMockUser(roles = "BOLETIM")
    void search_bookmarkFilter_isIgnoredForBoletim() throws Exception {
        PessoaFixture.builder(pessoaService)
                .nome("Pendencia Filtro A")
                .sexo(Sexo.FEMININO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
                .bookmark(true)
                .build();
        PessoaFixture.builder(pessoaService)
                .nome("Pendencia Filtro B")
                .sexo(Sexo.MASCULINO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
                .bookmark(false)
                .build();

        mockMvc.perform(post(PEOPLE + "/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Pendencia Filtro", "bookmark", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void search_bookmarkFilter_returnsOnlyMarkedForDeacon() throws Exception {
        PessoaFixture.builder(pessoaService)
                .nome("Pendencia Staff A")
                .sexo(Sexo.FEMININO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
                .bookmark(true)
                .build();
        PessoaFixture.builder(pessoaService)
                .nome("Pendencia Staff B")
                .sexo(Sexo.MASCULINO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
                .bookmark(false)
                .build();

        mockMvc.perform(post(PEOPLE + "/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Pendencia Staff", "bookmark", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bookmark").value(true));
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void generateReport_bookmarkFilter_isIgnoredForBoletim() throws Exception {
        PessoaFixture.builder(pessoaService)
                .nome("Pendencia Relatorio A")
                .sexo(Sexo.FEMININO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
                .bookmark(true)
                .build();
        PessoaFixture.builder(pessoaService)
                .nome("Pendencia Relatorio B")
                .sexo(Sexo.MASCULINO)
                .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
                .bookmark(false)
                .build();

        mockMvc.perform(post("/api/reports/generate-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nome", "Pendencia Relatorio", "bookmark", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void categories_areAllowedForBoletim() throws Exception {
        mockMvc.perform(get("/api/categorias")).andExpect(status().isOk());
        mockMvc.perform(get("/api/categorias/agregadores")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void reportSummary_isAllowedForBoletim() throws Exception {
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isOk());
    }

    // ===== escrita segue fechada =====

    @Test
    @WithMockUser(roles = "BOLETIM")
    void personHistory_isForbiddenForBoletim() throws Exception {
        mockMvc.perform(get(PEOPLE + "/{id}/history", 1))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void personHistory_isAllowedForDeacon() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Historico Diacono", Sexo.FEMININO);

        mockMvc.perform(get(PEOPLE + "/{id}/history", pessoa.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void personNotes_areForbiddenForBoletim() throws Exception {
        mockMvc.perform(get(PEOPLE + "/{id}/notes", 1))
                .andExpect(status().isForbidden());
    }

    @Test
    void personSearch_returnsUnauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(post(PEOPLE + "/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
