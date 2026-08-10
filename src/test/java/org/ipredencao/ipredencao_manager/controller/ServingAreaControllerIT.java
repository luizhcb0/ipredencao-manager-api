package org.ipredencao.ipredencao_manager.controller;

import com.fasterxml.jackson.databind.JsonNode;

import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.StreamSupport;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security matrix + round-trips for {@link ServingAreaController}. Leitura liberada
 * ao BOLETIM (inclui os POST de busca/relatório); escrita exige DIACONO+.
 */
@Transactional
class ServingAreaControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/serving-areas";

    @Autowired private PessoaService pessoaService;

    // ===== security matrix =====

    @Test
    void search_returnsUnauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(post(BASE + "/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void search_isAllowedForBoletim() throws Exception {
        mockMvc.perform(post(BASE + "/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void participationReport_isAllowedForBoletim() throws Exception {
        mockMvc.perform(post(BASE + "/participation-report").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void membersSearch_isAllowedForBoletim() throws Exception {
        mockMvc.perform(post(BASE + "/members/search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void create_isForbiddenForBoletim() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BOLETIM")
    void delete_isForbiddenForBoletim() throws Exception {
        mockMvc.perform(delete(BASE + "/1")).andExpect(status().isForbidden());
    }

    // ===== round-trips =====

    @Test
    @WithMockUser(roles = "DIACONO")
    void create_isAllowedForDiacono_andSeedsDefaultPositions() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ministério de Música\",\"description\":\"Louvor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ministério de Música"))
                .andExpect(jsonPath("$.positions", hasSize(3)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createThenFetchDetail_roundTrips() throws Exception {
        String body = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mocidade\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mocidade"))
                .andExpect(jsonPath("$.positions", hasSize(3)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withInvalidWhatsapp_returnsBadRequest() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"GF\",\"whatsappUrl\":\"http://x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void search_returnsListCounts() throws Exception {
        String created = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Contagens API\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long areaId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(post(BASE + "/search").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + areaId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].teamCount").value(0))
                .andExpect(jsonPath("$.data[0].memberCount").value(0))
                .andExpect(jsonPath("$.data[0].coordinatorCount").value(0));
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void addMember_membershipWithoutTeam_whenTeamsExist_returnsBadRequest() throws Exception {
        String created = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Com equipe API\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long areaId = objectMapper.readTree(created).get("id").asLong();
        JsonNode positions = objectMapper.readTree(created).get("positions");
        Long memberPosId = StreamSupport.stream(positions.spliterator(), false)
                .filter(p -> "MEMBERSHIP".equals(p.get("kind").asText()))
                .findFirst().orElseThrow()
                .get("id").asLong();

        mockMvc.perform(post(BASE + "/" + areaId + "/teams").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Equipe\"}"))
                .andExpect(status().isOk());

        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Membro API", Sexo.MASCULINO);

        mockMvc.perform(post(BASE + "/" + areaId + "/members").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"personId\":" + pessoa.getId() + ",\"positionId\":" + memberPosId + "}"))
                .andExpect(status().isBadRequest());
    }
}
