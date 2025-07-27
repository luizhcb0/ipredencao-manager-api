package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.FormularioPessoa;
import org.ipredencao.ipredencao_manager.repository.FormularioPessoaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FormularioPessoaService {
    @Autowired
    private FormularioPessoaRepository repository;

    public FormularioPessoa criar(FormularioPessoa formulario) {
        return repository.insert(formulario);
    }

    public FormularioPessoa atualizar(FormularioPessoa formulario) {
        return repository.update(formulario);
    }

    public List<FormularioPessoa> listarTodos() {
        return repository.findAll();
    }

    public FormularioPessoa buscarPorId(Long id) {
        return repository.findById(id);
    }
} 