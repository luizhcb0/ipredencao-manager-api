package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.Pessoa;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.stream.Collectors;
import org.joda.time.DateTime;

// Importe os records e tabelas do JOOQ gerados para formulario_pessoa
import static org.ipredencao.ipredencao_manager.jooq.tables.FormularioPessoa.FORMULARIO_PESSOA;
import org.ipredencao.ipredencao_manager.jooq.tables.records.FormularioPessoaRecord;

@Repository
public class FormularioPessoaRepository {
    @Autowired
    private DSLContext dsl;

    public FormularioPessoa insert(FormularioPessoa formulario) {
        FormularioPessoaRecord record = toRepository(formulario);
        FormularioPessoaRecord saved = dsl.insertInto(FORMULARIO_PESSOA)
                .set(record)
                .returning()
                .fetchOne();
        return fromRepository(saved);
    }

    public FormularioPessoa update(FormularioPessoa formulario) {
        FormularioPessoaRecord record = toRepository(formulario);
        FormularioPessoaRecord updated = dsl.update(FORMULARIO_PESSOA)
                .set(record)
                .set(FORMULARIO_PESSOA.UPDATED_AT, DateTimeHelper.toDb(DateTime.now()))
                .where(FORMULARIO_PESSOA.FORMULARIO_PESSOA_ID.eq(formulario.getId()))
                .returning()
                .fetchOne();
        return fromRepository(updated);
    }

    public List<FormularioPessoa> findAll() {
        return dsl.selectFrom(FORMULARIO_PESSOA)
                .fetch()
                .stream()
                .map(FormularioPessoaRepository::fromRepository)
                .collect(Collectors.toList());
    }

    public FormularioPessoa findById(Long id) {
        FormularioPessoaRecord record = dsl.selectFrom(FORMULARIO_PESSOA)
                .where(FORMULARIO_PESSOA.FORMULARIO_PESSOA_ID.eq(id))
                .fetchOne();
        return fromRepository(record);
    }

    private static FormularioPessoa fromRepository(FormularioPessoaRecord record) {
        if (record == null) return null;
        FormularioPessoa f = new FormularioPessoa();
        f.setId(record.getFormularioPessoaId());
        f.setNome(record.getNome());
        f.setApelido(record.getApelido());
        f.setEmail(record.getEmail());
        f.setTelefone(record.getTelefone());
        f.setCampus(record.getCampus());
        f.setDataNascimento(DateTimeHelper.fromDb(record.getDataNascimento()));
        f.setCpf(record.getCpf());
        f.setRg(record.getRg());
        if (record.getEstadoCivil() != null)
            f.setEstadoCivil(org.ipredencao.ipredencao_manager.model.EstadoCivil.valueOf(record.getEstadoCivil().name()));
        f.setIgrejaAnterior(record.getIgrejaAnterior());
        f.setSituacaoIgrejaAnterior(record.getSituacaoIgrejaAnterior());
        f.setTempoNaIgreja(record.getTempoNaIgreja());
        f.setMotivosParaAdmissao(record.getMotivosParaAdmissao());
        if (record.getTipoBatismo() != null)
            f.setTipoBatismo(org.ipredencao.ipredencao_manager.model.TipoBatismo.valueOf(record.getTipoBatismo().name()));
        f.setDataBatismo(DateTimeHelper.fromDb(record.getDataBatismo()));
        f.setDataProfissaoDeFe(DateTimeHelper.fromDb(record.getDataProfissaoDeFe()));
        f.setIgrejaBatismo(record.getIgrejaBatismo());
        f.setProfissao(record.getProfissao());
        f.setEmpresa(record.getEmpresa());
        f.setEndereco(record.getEndereco());
        f.setCep(record.getCep());
        if (record.getRegiao() != null)
            f.setRegiao(org.ipredencao.ipredencao_manager.model.Regiao.valueOf(record.getRegiao().name()));
        f.setLatitude(record.getLatitude() != null ? record.getLatitude().doubleValue() : null);
        f.setLongitude(record.getLongitude() != null ? record.getLongitude().doubleValue() : null);
        f.setFotoUrl(record.getFotoUrl());
        if (record.getSexo() != null)
            f.setSexo(org.ipredencao.ipredencao_manager.model.Sexo.valueOf(record.getSexo().name()));
        // chefeDeFamilia: no formulário é String, na model é Long. Não mapeia diretamente.
        if (record.getCategoriaId() != null)
            f.setSubcategoria(org.ipredencao.ipredencao_manager.model.SubcategoriaEnum.fromId(record.getCategoriaId()));
        if (record.getEmailsSecundarios() != null) {
            f.setEmailsSecundarios(java.util.Arrays.asList(record.getEmailsSecundarios()));
        }
        if (record.getTelefonesSecundarios() != null) {
            f.setTelefonesSecundarios(java.util.Arrays.asList(record.getTelefonesSecundarios()));
        }
        // Campos extras do formulário
        f.setNomeParceiro(record.getNomeParceiro());
        f.setNomePai(record.getNomePai());
        f.setNomeMae(record.getNomeMae());
        f.setNomeFilhos(record.getNomeFilhos() != null ? java.util.Arrays.asList(record.getNomeFilhos()) : null);
        if (record.getStatus() != null)
            f.setStatus(org.ipredencao.ipredencao_manager.model.FormPessoaStatus.valueOf(record.getStatus().name()));
        f.setAddedAt(DateTimeHelper.fromDb(record.getAddedAt()));
        f.setUpdatedAt(DateTimeHelper.fromDb(record.getUpdatedAt()));
        return f;
    }

    private static FormularioPessoaRecord toRepository(FormularioPessoa f) {
        FormularioPessoaRecord record = new FormularioPessoaRecord();
        if (f.getId() != null) record.setFormularioPessoaId(f.getId());
        record.setNome(f.getNome());
        record.setApelido(f.getApelido());
        record.setEmail(f.getEmail());
        record.setTelefone(f.getTelefone());
        record.setCampus(f.getCampus());
        record.setDataNascimento(DateTimeHelper.toDb(f.getDataNascimento()));
        record.setCpf(f.getCpf());
        record.setRg(f.getRg());
        if (f.getEstadoCivil() != null)
            record.setEstadoCivil(org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil.valueOf(f.getEstadoCivil().name()));
        record.setIgrejaAnterior(f.getIgrejaAnterior());
        record.setSituacaoIgrejaAnterior(f.getSituacaoIgrejaAnterior());
        record.setTempoNaIgreja(f.getTempoNaIgreja());
        record.setMotivosParaAdmissao(f.getMotivosParaAdmissao());
        if (f.getTipoBatismo() != null)
            record.setTipoBatismo(org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo.valueOf(f.getTipoBatismo().name()));
        record.setDataBatismo(DateTimeHelper.toDb(f.getDataBatismo()));
        record.setDataProfissaoDeFe(DateTimeHelper.toDb(f.getDataProfissaoDeFe()));
        record.setIgrejaBatismo(f.getIgrejaBatismo());
        record.setProfissao(f.getProfissao());
        record.setEmpresa(f.getEmpresa());
        record.setEndereco(f.getEndereco());
        record.setCep(f.getCep());
        if (f.getRegiao() != null)
            record.setRegiao(org.ipredencao.ipredencao_manager.jooq.enums.Regiao.valueOf(f.getRegiao().name()));
        if (f.getLatitude() != null) record.setLatitude(java.math.BigDecimal.valueOf(f.getLatitude()));
        if (f.getLongitude() != null) record.setLongitude(java.math.BigDecimal.valueOf(f.getLongitude()));
        record.setFotoUrl(f.getFotoUrl());
        if (f.getSexo() != null)
            record.setSexo(org.ipredencao.ipredencao_manager.jooq.enums.Sexo.valueOf(f.getSexo().name()));
        // chefeDeFamilia: no formulário é String, na model é Long. Não mapeia diretamente.
        if (f.getSubcategoria() != null) {
            record.setCategoriaId(f.getSubcategoria().getId());
        }
        if (f.getEmailsSecundarios() != null && !f.getEmailsSecundarios().isEmpty()) {
            record.setEmailsSecundarios(f.getEmailsSecundarios().toArray(new String[0]));
        }
        if (f.getTelefonesSecundarios() != null && !f.getTelefonesSecundarios().isEmpty()) {
            record.setTelefonesSecundarios(f.getTelefonesSecundarios().toArray(new String[0]));
        }
        // Campos extras do formulário
        record.setNomeParceiro(f.getNomeParceiro());
        record.setNomePai(f.getNomePai());
        record.setNomeMae(f.getNomeMae());
        if (f.getNomeFilhos() != null)
            record.setNomeFilhos(f.getNomeFilhos().toArray(new String[0]));
        if (f.getStatus() != null)
            record.setStatus(org.ipredencao.ipredencao_manager.jooq.enums.FormPessoaStatus.valueOf(f.getStatus().name()));
        record.setAddedAt(DateTimeHelper.toDb(f.getAddedAt()));
        record.setUpdatedAt(DateTimeHelper.toDb(f.getUpdatedAt()));
        return record;
    }
} 