package org.ipredencao.ipredencao_manager;

import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/**
 * Smoke test: garante que o contexto Spring sobe corretamente contra o container
 * Postgres provisionado pelo {@link IntegrationTestBase}.
 */
class IpredencaoManagerApplicationTests extends IntegrationTestBase {

    @Test
    void contextLoads() {
    }
}
