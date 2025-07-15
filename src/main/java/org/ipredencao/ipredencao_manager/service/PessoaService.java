package org.ipredencao.ipredencao_manager.service;

import org.springframework.stereotype.Service;
import org.ipredencao.ipredencao_manager.repository.PessoaRepository;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.model.RelacionamentoPessoa;

import java.util.List;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import com.amazonaws.services.s3.model.ObjectMetadata;

@Service
public class PessoaService {
    private final PessoaRepository pessoaRepository;

    @Autowired
    private AmazonS3 amazonS3;
    private final String bucketName = "ipredencao-manager-photos";

    public PessoaService(PessoaRepository pessoaRepository) {this.pessoaRepository = pessoaRepository;}

    public Pessoa criarPessoa(Pessoa pessoa) {
        return pessoaRepository.inserirPessoa(pessoa);
    }

    public Pessoa criarPessoaComFoto(Long idPessoa, MultipartFile foto) throws IOException {
        if (foto != null && !foto.isEmpty()) {
            String key = idPessoa + "_" + System.currentTimeMillis() + "_" + foto.getOriginalFilename();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(foto.getSize());
            amazonS3.putObject(new PutObjectRequest(bucketName, key, foto.getInputStream(), metadata));
            String url = amazonS3.getUrl(bucketName, key).toString();
            Pessoa pessoa = pessoaRepository.buscarPorId(idPessoa);
            pessoa.setFotoUrl(url);
            pessoaRepository.atualizarPessoa(pessoa);
        }
        return pessoaRepository.buscarPorId(idPessoa);
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