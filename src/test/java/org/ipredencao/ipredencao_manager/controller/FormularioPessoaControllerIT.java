package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class FormularioPessoaControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/formulario-pessoa";

    @Test
    @WithMockUser(roles = "DIACONO")
    void findById_includesUpdatedAtFromDatabase() throws Exception {
        String created = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publicFormBody("Formulário Timestamp")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
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
}
