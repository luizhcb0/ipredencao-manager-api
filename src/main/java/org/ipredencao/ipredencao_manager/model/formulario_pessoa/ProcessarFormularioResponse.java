package org.ipredencao.ipredencao_manager.model.formulario_pessoa;

import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;

import java.util.List;

public record ProcessarFormularioResponse(
    Pessoa pessoaPrincipal,
    List<Relacionamento> relacionamentosCriados,
    String mensagem
) {}
