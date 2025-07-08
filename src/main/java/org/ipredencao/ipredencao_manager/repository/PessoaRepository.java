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
import org.ipredencao.ipredencao_manager.model.TipoRelacionamento;
import org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil;
import org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo;
import org.ipredencao.ipredencao_manager.jooq.enums.EstadoPessoa;
import org.ipredencao.ipredencao_manager.jooq.enums.TipoAdmissao;

@Repository
public class PessoaRepository {
    @Autowired
    private DSLContext dsl;

    

    private static Pessoa fromRepository(PessoaRecord record) {
        if (record == null) return null;
        Pessoa p = new Pessoa();
        p.setId(record.getId());
        p.setNome(record.getNome());
        p.setSedeCongregacao(record.getSedeCongregacao());
        p.setApelido(record.getApelido());
        p.setDataNascimento(DateTimeHelper.fromDb(record.getDataNascimento()));
        p.setTelefone(record.getTelefone());
        if (record.getEstadoCivil() != null)
            p.setEstadoCivil(org.ipredencao.ipredencao_manager.model.EstadoCivil.valueOf(record.getEstadoCivil().name()));
        p.setIgrejaAnterior(record.getIgrejaAnterior());
        p.setSituacaoIgrejaAnterior(record.getSituacaoIgrejaAnterior());
        p.setTempoNaIpr(record.getTempoNaIpr());
        p.setMotivosAdmissao(record.getMotivosAdmissao());
        if (record.getTipoBatismo() != null)
            p.setTipoBatismo(org.ipredencao.ipredencao_manager.model.TipoBatismo.valueOf(record.getTipoBatismo().name()));
        p.setDataBatismo(DateTimeHelper.fromDb(record.getDataBatismo()));
        p.setIgrejaBatismo(record.getIgrejaBatismo());
        p.setDadosOficial(record.getDadosOficial());
        p.setSupervisoes(record.getSupervisoes());
        p.setOutrasCategorias(record.getOutrasCategorias());
        if (record.getEstadoPessoa() != null)
            p.setEstadoPessoa(org.ipredencao.ipredencao_manager.model.EstadoPessoa.valueOf(record.getEstadoPessoa().name()));
        if (record.getTipoAdmissao() != null)
            p.setTipoAdmissao(org.ipredencao.ipredencao_manager.model.TipoAdmissao.valueOf(record.getTipoAdmissao().name()));
        p.setCpf(record.getCpf());
        p.setRg(record.getRg());
        p.setEmailAdicional(record.getEmailAdicional());
        p.setTelefoneAdicional(record.getTelefoneAdicional());
        p.setEmailTrabalho(record.getEmailTrabalho());
        p.setTelefoneTrabalho(record.getTelefoneTrabalho());
        p.setProfissao(record.getProfissao());
        p.setEmpresa(record.getEmpresa());
        p.setEndereco(record.getEndereco());
        p.setLatitude(record.getLatitude() != null ? record.getLatitude().doubleValue() : null);
        p.setLongitude(record.getLongitude() != null ? record.getLongitude().doubleValue() : null);
        p.setRegiaoGf(record.getRegiaoGf());
        p.setSkype(record.getSkype());
        p.setTwitter(record.getTwitter());
        p.setLinkedin(record.getLinkedin());
        p.setInstagram(record.getInstagram());
        p.setFacebook(record.getFacebook());
        p.setPaginaPessoal(record.getPaginaPessoal());
        return p;
    }

    public Pessoa inserirPessoa(Pessoa pessoa) {
        PessoaRecord record = toRepository(pessoa);
        PessoaRecord saved = dsl.insertInto(PESSOA)
                .set(record)
                .returning()
                .fetchOne();
        return fromRepository(saved);
    }

    public Pessoa buscarPorId(Long id) {
        PessoaRecord record = dsl.selectFrom(PESSOA)
                .where(PESSOA.ID.eq(id))
                .fetchOne();
        return fromRepository(record);
    }

    public List<Pessoa> listarTodas() {
        return dsl.selectFrom(PESSOA)
                .fetch()
                .stream()
                .map(PessoaRepository::fromRepository)
                .collect(Collectors.toList());
    }

    public int atualizarPessoa(Pessoa pessoa) {
        PessoaRecord record = toRepository(pessoa);
        return dsl.update(PESSOA)
                .set(record)
                .where(PESSOA.ID.eq(pessoa.getId()))
                .execute();
    }

    public int deletarPessoa(Long id) {
        return dsl.deleteFrom(PESSOA)
                .where(PESSOA.ID.eq(id))
                .execute();
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

    public int deletarRelacionamento(Long relacionamentoId) {
        return dsl.deleteFrom(PESSOA_RELACIONAMENTO)
                .where(PESSOA_RELACIONAMENTO.ID.eq(relacionamentoId))
                .execute();
    }

    private static PessoaRecord toRepository(Pessoa pessoa) {
        PessoaRecord record = new PessoaRecord();
        record.setId(pessoa.getId());
        record.setNome(pessoa.getNome());
        record.setSedeCongregacao(pessoa.getSedeCongregacao());
        record.setApelido(pessoa.getApelido());
        record.setDataNascimento(DateTimeHelper.toDb(pessoa.getDataNascimento()));
        record.setTelefone(pessoa.getTelefone());
        if (pessoa.getEstadoCivil() != null)
            record.setEstadoCivil(EstadoCivil.valueOf(pessoa.getEstadoCivil().name()));
        record.setIgrejaAnterior(pessoa.getIgrejaAnterior());
        record.setSituacaoIgrejaAnterior(pessoa.getSituacaoIgrejaAnterior());
        record.setTempoNaIpr(pessoa.getTempoNaIpr());
        record.setMotivosAdmissao(pessoa.getMotivosAdmissao());
        if (pessoa.getTipoBatismo() != null)
            record.setTipoBatismo(TipoBatismo.valueOf(pessoa.getTipoBatismo().name()));
        record.setDataBatismo(DateTimeHelper.toDb(pessoa.getDataBatismo()));
        record.setIgrejaBatismo(pessoa.getIgrejaBatismo());
        record.setDadosOficial(pessoa.getDadosOficial());
        record.setSupervisoes(pessoa.getSupervisoes());
        record.setOutrasCategorias(pessoa.getOutrasCategorias());
        if (pessoa.getEstadoPessoa() != null)
            record.setEstadoPessoa(EstadoPessoa.valueOf(pessoa.getEstadoPessoa().name()));
        if (pessoa.getTipoAdmissao() != null)
            record.setTipoAdmissao(TipoAdmissao.valueOf(pessoa.getTipoAdmissao().name()));
        record.setCpf(pessoa.getCpf());
        record.setRg(pessoa.getRg());
        record.setEmailAdicional(pessoa.getEmailAdicional());
        record.setTelefoneAdicional(pessoa.getTelefoneAdicional());
        record.setEmailTrabalho(pessoa.getEmailTrabalho());
        record.setTelefoneTrabalho(pessoa.getTelefoneTrabalho());
        record.setProfissao(pessoa.getProfissao());
        record.setEmpresa(pessoa.getEmpresa());
        record.setEndereco(pessoa.getEndereco());
        if (pessoa.getLatitude() != null) record.setLatitude(java.math.BigDecimal.valueOf(pessoa.getLatitude()));
        if (pessoa.getLongitude() != null) record.setLongitude(java.math.BigDecimal.valueOf(pessoa.getLongitude()));
        record.setRegiaoGf(pessoa.getRegiaoGf());
        record.setSkype(pessoa.getSkype());
        record.setTwitter(pessoa.getTwitter());
        record.setLinkedin(pessoa.getLinkedin());
        record.setInstagram(pessoa.getInstagram());
        record.setFacebook(pessoa.getFacebook());
        record.setPaginaPessoal(pessoa.getPaginaPessoal());
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
            rel.setTipoRelacionamento(TipoRelacionamento.valueOf(record.getTipoRelacionamento()));
        rel.setInicioRelacionamento(DateTimeHelper.fromDb(record.getInicioRelacionamento()));
        return rel;
    }

    private static PessoaRelacionamentoRecord toRepository(RelacionamentoPessoa relacionamento) {
        PessoaRelacionamentoRecord record = new PessoaRelacionamentoRecord();
        record.setId(relacionamento.getId());
        if (relacionamento.getPessoaRelacionada() != null)
            record.setPessoaRelacionadaId(relacionamento.getPessoaRelacionada().getId());
        if (relacionamento.getTipoRelacionamento() != null)
            record.setTipoRelacionamento(relacionamento.getTipoRelacionamento().name());
        record.setInicioRelacionamento(DateTimeHelper.toDb(relacionamento.getInicioRelacionamento()));
        return record;
    }
} 