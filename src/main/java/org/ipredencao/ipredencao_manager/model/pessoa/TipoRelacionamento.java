package org.ipredencao.ipredencao_manager.model.pessoa;

import java.util.List;

public enum TipoRelacionamento {
    SEM_RELACIONAMENTO,
    CONJUGE,
    NOIVO,
    NAMORADO,
    FILHO,
    PAI,
    MAE,
    IRMAO,
    RESPONSAVEL,
    VIUVO;

    public static TipoRelacionamento inverter(TipoRelacionamento tipo, Sexo sexo) {
        return switch (tipo) {
            case SEM_RELACIONAMENTO, CONJUGE, NOIVO, NAMORADO, IRMAO, VIUVO -> tipo;
            case FILHO -> (sexo == Sexo.FEMININO) ? MAE : PAI;
            case PAI, MAE, RESPONSAVEL -> FILHO;
        };
    }

    public static List<TipoRelacionamento> getInversos(TipoRelacionamento tipo) {
        return switch (tipo) {
            case SEM_RELACIONAMENTO, CONJUGE, NOIVO, NAMORADO, IRMAO, VIUVO -> List.of(tipo);
            case FILHO -> List.of(PAI, MAE);
            case PAI, MAE, RESPONSAVEL -> List.of(FILHO);
        };
    }
}
