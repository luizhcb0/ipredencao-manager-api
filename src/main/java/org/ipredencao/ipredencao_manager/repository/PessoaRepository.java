package org.ipredencao.ipredencao_manager.repository;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.model.RelacionamentoPessoa;
import static org.ipredencao.ipredencao_manager.jooq.tables.Pessoa.PESSOA;
import static org.ipredencao.ipredencao_manager.jooq.tables.PessoaRelacionamento.PESSOA_RELACIONAMENTO;
import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaRecord;
import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaRelacionamentoRecord;
import java.util.List;
import java.util.stream.Collectors;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil;
import org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ipredencao.ipredencao_manager.model.PessoaQuery;
import org.jooq.Condition;
import org.jooq.impl.DSL;

@Repository
public class PessoaRepository {
    @Autowired
    private DSLContext dsl;


    public Pessoa insert(Pessoa pessoa) {
        PessoaRecord record = toRepository(pessoa);
        PessoaRecord saved = dsl.insertInto(PESSOA)
                .set(record)
                .returning()
                .fetchOne();
        return fromRepository(saved);
    }

    public Pessoa update(Pessoa pessoa) {
        PessoaRecord record = toRepository(pessoa);
        dsl.update(PESSOA)
                .set(record)
                .where(PESSOA.ID.eq(pessoa.getId()))
                .execute();
        return pessoa;
    }

    public List<Pessoa> find(PessoaQuery query) {
        List<Condition> conditions = buildConditions(query);
        
        Condition finalCondition = conditions.stream()
            .reduce(DSL.noCondition(), (a, b) -> a.and(b));
        
        return dsl.selectFrom(PESSOA)
                .where(finalCondition)
                .fetch()
                .stream()
                .map(PessoaRepository::fromRepository)
                .collect(Collectors.toList());
    }

    // CRUD para relacionamentos qualificados
    public RelacionamentoPessoa inserirRelacionamento(Long pessoaId, RelacionamentoPessoa relacionamento) {
        PessoaRelacionamentoRecord record = toRepository(relacionamento);
        record.setPessoaId(pessoaId);
        PessoaRelacionamentoRecord saved = dsl.insertInto(PESSOA_RELACIONAMENTO)
                .set(record)
                .returning()
                .fetchOne();
        return fromRepository(saved);
    }

    public List<RelacionamentoPessoa> listarRelacionamentosPorPessoa(Long pessoaId) {
        return dsl.selectFrom(PESSOA_RELACIONAMENTO)
                .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(pessoaId))
                .fetch()
                .stream()
                .map(PessoaRepository::fromRepository)
                .collect(Collectors.toList());
    }
    

    private List<Condition> buildConditions(PessoaQuery query) {
        List<Condition> conditions = new java.util.ArrayList<>();
        
        query.getId().ifPresent(id -> conditions.add(PESSOA.ID.eq(id)));
        query.getIds().ifPresent(ids -> conditions.add(PESSOA.ID.in(ids)));
        query.getNome().ifPresent(nome -> conditions.add(PESSOA.NOME.like("%" + nome + "%")));
        query.getApelido().ifPresent(apelido -> conditions.add(PESSOA.APELIDO.like("%" + apelido + "%")));
        query.getEmail().ifPresent(email -> conditions.add(PESSOA.EMAIL.eq(email)));
        query.getTelefone().ifPresent(telefone -> conditions.add(PESSOA.TELEFONE.eq(telefone)));
        query.getCpf().ifPresent(cpf -> conditions.add(PESSOA.CPF.eq(cpf)));
        query.getRg().ifPresent(rg -> conditions.add(PESSOA.RG.eq(rg)));
        query.getEstadoCivil().ifPresent(estadoCivil -> 
            conditions.add(PESSOA.ESTADO_CIVIL.eq(org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil.valueOf(estadoCivil.name()))));
        query.getCampus().ifPresent(campus -> conditions.add(PESSOA.CAMPUS.eq(campus)));
        query.getRegiao().ifPresent(regiao -> 
            conditions.add(PESSOA.REGIAO.eq(org.ipredencao.ipredencao_manager.jooq.enums.Regiao.valueOf(regiao.name()))));
        query.getStatus().ifPresent(status -> 
            conditions.add(PESSOA.STATUS.eq(org.ipredencao.ipredencao_manager.jooq.enums.Status.valueOf(status.name()))));
        query.getDataNascimentoFrom().ifPresent(from -> 
            conditions.add(PESSOA.DATA_NASCIMENTO.greaterOrEqual(DateTimeHelper.toDb(from))));
        query.getDataNascimentoTo().ifPresent(to -> 
            conditions.add(PESSOA.DATA_NASCIMENTO.lessOrEqual(DateTimeHelper.toDb(to))));
        query.getTipoBatismo().ifPresent(tipoBatismo -> 
            conditions.add(PESSOA.TIPO_BATISMO.eq(org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo.valueOf(tipoBatismo.name()))));
        
        return conditions;
    }

    private static Pessoa fromRepository(PessoaRecord record) {
        if (record == null) return null;
        Pessoa p = new Pessoa();
        p.setId(record.getId());
        p.setNome(record.getNome());
        p.setApelido(record.getApelido());
        p.setEmail(record.getEmail());
        p.setTelefone(record.getTelefone());
        p.setCampus(record.getCampus());
        p.setDataNascimento(DateTimeHelper.fromDb(record.getDataNascimento()));
        p.setCpf(record.getCpf());
        p.setRg(record.getRg());
        if (record.getEstadoCivil() != null)
            p.setEstadoCivil(org.ipredencao.ipredencao_manager.model.EstadoCivil.valueOf(record.getEstadoCivil().name()));
        p.setIgrejaAnterior(record.getIgrejaAnterior());
        p.setSituacaoIgrejaAnterior(record.getSituacaoIgrejaAnterior());
        p.setTempoNaIgreja(record.getTempoNaIgreja());
        p.setMotivosParaAdmissao(record.getMotivosParaAdmissao());
        if (record.getTipoBatismo() != null)
            p.setTipoBatismo(org.ipredencao.ipredencao_manager.model.TipoBatismo.valueOf(record.getTipoBatismo().name()));
        p.setDataBatismo(DateTimeHelper.fromDb(record.getDataBatismo()));
        p.setDataProfissaoDeFe(DateTimeHelper.fromDb(record.getDataProfissaoDeFe()));
        p.setIgrejaBatismo(record.getIgrejaBatismo());
        p.setProfissao(record.getProfissao());
        p.setEmpresa(record.getEmpresa());
        p.setEndereco(record.getEndereco());
        if (record.getRegiao() != null)
            p.setRegiao(org.ipredencao.ipredencao_manager.model.Regiao.valueOf(record.getRegiao().name()));
        p.setLatitude(record.getLatitude() != null ? record.getLatitude().doubleValue() : null);
        p.setLongitude(record.getLongitude() != null ? record.getLongitude().doubleValue() : null);
        p.setFotoUrl(record.getFotoUrl());
        if (record.getStatus() != null)
            p.setStatus(org.ipredencao.ipredencao_manager.model.Status.valueOf(record.getStatus().name()));
        
        // Converter arrays do PostgreSQL para List<String>
        if (record.getEmailsSecundarios() != null) {
            p.setEmailsSecundarios(java.util.Arrays.asList(record.getEmailsSecundarios()));
        }
        if (record.getTelefonesSecundarios() != null) {
            p.setTelefonesSecundarios(java.util.Arrays.asList(record.getTelefonesSecundarios()));
        }
        
        return p;
    }

    private static PessoaRecord toRepository(Pessoa pessoa) {
        PessoaRecord record = new PessoaRecord();
        record.setId(pessoa.getId());
        record.setNome(pessoa.getNome());
        record.setApelido(pessoa.getApelido());
        record.setEmail(pessoa.getEmail());
        record.setTelefone(pessoa.getTelefone());
        record.setCampus(pessoa.getCampus());
        record.setDataNascimento(DateTimeHelper.toDb(pessoa.getDataNascimento()));
        record.setCpf(pessoa.getCpf());
        record.setRg(pessoa.getRg());
        if (pessoa.getEstadoCivil() != null)
            record.setEstadoCivil(EstadoCivil.valueOf(pessoa.getEstadoCivil().name()));
        record.setIgrejaAnterior(pessoa.getIgrejaAnterior());
        record.setSituacaoIgrejaAnterior(pessoa.getSituacaoIgrejaAnterior());
        record.setTempoNaIgreja(pessoa.getTempoNaIgreja());
        record.setMotivosParaAdmissao(pessoa.getMotivosParaAdmissao());
        if (pessoa.getTipoBatismo() != null)
            record.setTipoBatismo(TipoBatismo.valueOf(pessoa.getTipoBatismo().name()));
        record.setDataBatismo(DateTimeHelper.toDb(pessoa.getDataBatismo()));
        record.setDataProfissaoDeFe(DateTimeHelper.toDb(pessoa.getDataProfissaoDeFe()));
        record.setIgrejaBatismo(pessoa.getIgrejaBatismo());
        record.setProfissao(pessoa.getProfissao());
        record.setEmpresa(pessoa.getEmpresa());
        record.setEndereco(pessoa.getEndereco());
        if (pessoa.getRegiao() != null)
            record.setRegiao(org.ipredencao.ipredencao_manager.jooq.enums.Regiao.valueOf(pessoa.getRegiao().name()));
        if (pessoa.getLatitude() != null) record.setLatitude(java.math.BigDecimal.valueOf(pessoa.getLatitude()));
        if (pessoa.getLongitude() != null) record.setLongitude(java.math.BigDecimal.valueOf(pessoa.getLongitude()));
        record.setFotoUrl(pessoa.getFotoUrl());
        if (pessoa.getStatus() != null)
            record.setStatus(org.ipredencao.ipredencao_manager.jooq.enums.Status.valueOf(pessoa.getStatus().name()));
        
        // Converter List<String> para arrays do PostgreSQL
        if (pessoa.getEmailsSecundarios() != null && !pessoa.getEmailsSecundarios().isEmpty()) {
            record.setEmailsSecundarios(pessoa.getEmailsSecundarios().toArray(new String[0]));
        }
        if (pessoa.getTelefonesSecundarios() != null && !pessoa.getTelefonesSecundarios().isEmpty()) {
            record.setTelefonesSecundarios(pessoa.getTelefonesSecundarios().toArray(new String[0]));
        }
        
        return record;
    }

    private static RelacionamentoPessoa fromRepository(PessoaRelacionamentoRecord record) {
        if (record == null) return null;
        RelacionamentoPessoa rel = new RelacionamentoPessoa();
        rel.setId(record.getId());
        Pessoa pessoaRelacionada = new Pessoa();
        pessoaRelacionada.setId(record.getPessoaRelacionadaId());
        rel.setPessoaRelacionada(pessoaRelacionada);
        if (record.getTipoRelacionamento() != null)
            rel.setTipoRelacionamento(
                org.ipredencao.ipredencao_manager.model.TipoRelacionamento.valueOf(
                    record.getTipoRelacionamento().name()
                )
            );
        rel.setInicioRelacionamento(DateTimeHelper.fromDb(record.getInicioRelacionamento()));
        return rel;
    }

    private static PessoaRelacionamentoRecord toRepository(RelacionamentoPessoa relacionamento) {
        PessoaRelacionamentoRecord record = new PessoaRelacionamentoRecord();
        record.setId(relacionamento.getId());
        if (relacionamento.getPessoaRelacionada() != null)
            record.setPessoaRelacionadaId(relacionamento.getPessoaRelacionada().getId());
        if (relacionamento.getTipoRelacionamento() != null)
            record.setTipoRelacionamento(
                org.ipredencao.ipredencao_manager.jooq.enums.TipoRelacionamento.valueOf(
                    relacionamento.getTipoRelacionamento().name()
                )
            );
        record.setInicioRelacionamento(DateTimeHelper.toDb(relacionamento.getInicioRelacionamento()));
        return record;
    }
} 