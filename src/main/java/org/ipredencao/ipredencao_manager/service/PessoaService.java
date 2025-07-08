package org.ipredencao.ipredencao_manager.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ipredencao.ipredencao_manager.repository.PessoaRepository;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.model.RelacionamentoPessoa;

import java.util.List;

@Service
public class PessoaService {
    @Autowired
    private PessoaRepository pessoaRepository;

    public Pessoa criarPessoa(Pessoa pessoa) {
        return pessoaRepository.inserirPessoa(pessoa);
    }

    public Pessoa buscarPorId(Long id) {
        return pessoaRepository.buscarPorId(id);
    }

    public List<Pessoa> listarTodas() {
        return pessoaRepository.listarTodas();
    }

    public int atualizarPessoa(Pessoa pessoa) {
        return pessoaRepository.atualizarPessoa(pessoa);
    }

    public int deletarPessoa(Long id) {
        return pessoaRepository.deletarPessoa(id);
    }

    // Relacionamentos qualificados
    public RelacionamentoPessoa criarRelacionamento(Long pessoaId, RelacionamentoPessoa relacionamento) {
        return pessoaRepository.inserirRelacionamento(pessoaId, relacionamento);
    }

    public List<RelacionamentoPessoa> listarRelacionamentosPorPessoa(Long pessoaId) {
        return pessoaRepository.listarRelacionamentosPorPessoa(pessoaId);
    }

    public int deletarRelacionamento(Long relacionamentoId) {
        return pessoaRepository.deletarRelacionamento(relacionamentoId);
    }
} 