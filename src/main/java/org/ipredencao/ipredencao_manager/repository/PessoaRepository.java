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
        p.setDataNascimento(record.getDataNascimento());
        p.setTelefone(record.getTelefone());
        p.setEstadoCivil(record.getEstadoCivil() != null ? record.getEstadoCivil().getLiteral() : null);
        p.setIgrejaAnterior(record.getIgrejaAnterior());
        p.setSituacaoIgrejaAnterior(record.getSituacaoIgrejaAnterior());
        p.setTempoNaIpr(record.getTempoNaIpr());
        p.setMotivosAdmissao(record.getMotivosAdmissao());
        p.setTipoBatismo(record.getTipoBatismo() != null ? record.getTipoBatismo().getLiteral() : null);
        p.setDataBatismo(record.getDataBatismo());
        p.setIgrejaBatismo(record.getIgrejaBatismo());
        p.setDadosOficial(record.getDadosOficial());
        p.setSupervisoes(record.getSupervisoes());
        p.setOutrasCategorias(record.getOutrasCategorias());
        p.setEstadoPessoa(record.getEstadoPessoa() != null ? record.getEstadoPessoa().getLiteral() : null);
        p.setTipoAdmissao(record.getTipoAdmissao() != null ? record.getTipoAdmissao().getLiteral() : null);
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
        if (pessoa.getDataNascimento() != null) record.setDataNascimento(pessoa.getDataNascimento());
        record.setTelefone(pessoa.getTelefone());
        // enums: estadoCivil, tipoBatismo, estadoPessoa, tipoAdmissao
        record.setIgrejaAnterior(pessoa.getIgrejaAnterior());
        record.setSituacaoIgrejaAnterior(pessoa.getSituacaoIgrejaAnterior());
        record.setTempoNaIpr(pessoa.getTempoNaIpr());
        record.setMotivosAdmissao(pessoa.getMotivosAdmissao());
        if (pessoa.getDataBatismo() != null) record.setDataBatismo(pessoa.getDataBatismo());
        record.setIgrejaBatismo(pessoa.getIgrejaBatismo());
        record.setDadosOficial(pessoa.getDadosOficial());
        record.setSupervisoes(pessoa.getSupervisoes());
        record.setOutrasCategorias(pessoa.getOutrasCategorias());
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
        // Apenas o id da pessoa relacionada é retornado, não o objeto completo
        Pessoa pessoaRelacionada = new Pessoa();
        pessoaRelacionada.setId(record.getPessoaRelacionadaId());
        rel.setPessoaRelacionada(pessoaRelacionada);
        rel.setTipoRelacionamento(record.getTipoRelacionamento());
        return rel;
    }
} 