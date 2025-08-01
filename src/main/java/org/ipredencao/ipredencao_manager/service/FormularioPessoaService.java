package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.FormularioPessoaQuery;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.repository.FormularioPessoaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FormularioPessoaService {
    @Autowired
    private FormularioPessoaRepository repository;
    @Autowired
    private S3Service s3Service;

    public FormularioPessoa criar(FormularioPessoa formulario) {
        // Validar se o email já existe
        if (!repository.find(FormularioPessoaQuery.builder().email(formulario.getEmail()).build()).isEmpty()) {
            throw new IllegalArgumentException("Já existe um formulário cadastrado com este email: " + formulario.getEmail());
        }
        return repository.insert(formulario);
    }

    public FormularioPessoa savePhoto(FormularioPessoa formulario, MultipartFile foto) throws IOException {
        String fotoUrl = s3Service.uploadPhoto(formulario.getId(), foto);
        formulario.setFotoUrl(fotoUrl);
        return repository.update(formulario);
    }

    public FormularioPessoa atualizar(FormularioPessoa formulario) {
        // Validar se o email já existe para outro formulário
        if (formulario.getId() != null && !repository.find(FormularioPessoaQuery.builder().email(formulario.getEmail()).build()).isEmpty()) {
            throw new IllegalArgumentException("Já existe outro formulário cadastrado com este email: " + formulario.getEmail());
        }
        return repository.update(formulario);
    }

    public FormularioPessoa findById(Long id) {
        try {
            return repository.find(FormularioPessoaQuery.builder().id(id).build()).getFirst();
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Formulario Pessoa com ID " + id + " não encontrada");
        }
    }

    public List<FormularioPessoa> find(FormularioPessoaQuery query) {
        return repository.find(query);
    }
} 