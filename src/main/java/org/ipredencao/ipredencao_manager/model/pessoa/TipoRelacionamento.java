package org.ipredencao.ipredencao_manager.model.pessoa;

import java.util.List;

public enum TipoRelacionamento {
    CONJUGE,
    NOIVO,
    NAMORADO,
    FILHO,
    PAI,
    MAE,
    IRMAO,
    RESPONSAVEL,
    VIUVO;

    public static TipoRelacionamento invert(TipoRelacionamento tipo, Sexo sexo) {
        return switch (tipo) {
            case CONJUGE, NOIVO, NAMORADO, IRMAO, VIUVO -> tipo;
            case FILHO -> (sexo == Sexo.FEMININO) ? MAE : PAI;
            case PAI, MAE, RESPONSAVEL -> FILHO;
        };
    }

    public static List<TipoRelacionamento> getInverses(TipoRelacionamento tipo) {
        return switch (tipo) {
            case CONJUGE, NOIVO, NAMORADO, IRMAO, VIUVO -> List.of(tipo);
            case FILHO -> List.of(PAI, MAE);
            case PAI, MAE, RESPONSAVEL -> List.of(FILHO);
        };
    }
}
