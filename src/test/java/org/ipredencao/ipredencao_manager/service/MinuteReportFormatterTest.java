package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MinuteReportFormatterTest {

    private MinuteReportFormatter formatter;
    private Pessoa pessoa;

    @BeforeEach
    void setUp() {
        formatter = new MinuteReportFormatter();
        pessoa = new Pessoa();
        pessoa.setNome("Maria Silva");
        pessoa.setSexo(Sexo.FEMININO);
    }

    @Test
    void batismoInfancia_usesCelebradoPor_andOmitsNaoInformado() {
        OfficialAct act = act(OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA,
                Map.of("celebrant", Map.of("name", "Não informado")));

        assertThat(formatter.format(act, pessoa))
                .endsWith("por batismo, celebrado por [celebrante].");
    }

    @Test
    void batismoInfancia_includesCelebrantName() {
        OfficialAct act = act(OfficialActFormEnum.ADM_MNC_BATISMO_INFANCIA,
                Map.of("celebrant", Map.of("name", "Rev. João")));

        assertThat(formatter.format(act, pessoa))
                .contains("por batismo, celebrado por Rev. João");
    }

    @Test
    void profissaoFeEBatismo_usesCelebradoPor() {
        OfficialAct act = act(OfficialActFormEnum.ADM_MC_PROFISSAO_FE_E_BATISMO,
                Map.of("celebrant", Map.of("name", "Rev. Pedro")));

        assertThat(formatter.format(act, pessoa))
                .contains("por profissão de fé e batismo, celebrado por Rev. Pedro");
    }

    @Test
    void demissaoCartaTransferencia_omitsDestinationWhenNaoInformado() {
        OfficialAct act = act(OfficialActFormEnum.DEM_MC_CARTA_TRANSFERENCIA,
                Map.of("destinationChurch", "Não informado"));

        assertThat(formatter.format(act, pessoa))
                .endsWith("por carta de transferência.");
    }

    @Test
    void demissaoCartaTransferencia_includesDestinationWhenPresent() {
        OfficialAct act = act(OfficialActFormEnum.DEM_MC_CARTA_TRANSFERENCIA,
                Map.of("destinationChurch", "Igreja Presbiteriana Central"));

        assertThat(formatter.format(act, pessoa))
                .contains("por carta de transferência, destinada à Igreja Presbiteriana Central");
    }

    private OfficialAct act(OfficialActFormEnum form, Map<String, Object> metadata) {
        OfficialAct act = new OfficialAct();
        act.setOfficialActFormId(form.getId());
        act.setActDate(new LocalDate(2024, 6, 1));
        act.setAdmissionOrderNumber(42L);
        act.setMetadata(metadata);
        return act;
    }
}
