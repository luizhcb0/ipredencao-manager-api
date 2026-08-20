package org.ipredencao.ipredencao_manager.controller;

import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O health check do App Runner chama {@code /actuator/health/liveness} sem credencial.
 * Se esse path deixar de ser publico, todo check responde 401, a instancia e marcada
 * unhealthy e o servico entra em loop de substituicao em producao - por isso o path
 * precisa de teste proprio, e nao so a rota /actuator/health.
 *
 * O grupo liveness so existe em application-prod.properties, entao aqui ele e definido
 * via properties para que o teste exercite o mesmo path que o App Runner chama.
 */
@SpringBootTest(properties = "management.endpoint.health.group.liveness.include=ping")
class ActuatorHealthSecurityIT extends IntegrationTestBase {

    @Test
    void liveness_isPublicAndReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk());
    }

    @Test
    void health_staysPublicForDiagnostics() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void otherActuatorEndpoints_stayProtected() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }
}
