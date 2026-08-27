package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaHistoryRecord;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistory;
import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistoryChange;
import org.ipredencao.ipredencao_manager.model.pessoa.pessoa_history.PessoaHistoryDiff;
import java.util.*;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.ipredencao.ipredencao_manager.model.pessoa.ChefeDeFamiliaRef;
import org.ipredencao.ipredencao_manager.model.pessoa.ConfidentialAccess;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaInclude;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoRelacionamento;

import static org.ipredencao.ipredencao_manager.jooq.Tables.PESSOA_HISTORY;
import static org.ipredencao.ipredencao_manager.jooq.tables.Endereco.ENDERECO;
import static org.ipredencao.ipredencao_manager.jooq.tables.Pessoa.PESSOA;
import static org.ipredencao.ipredencao_manager.jooq.tables.PessoaRelacionamento.PESSOA_RELACIONAMENTO;
import static org.ipredencao.ipredencao_manager.jooq.tables.Usuario.USUARIO;
import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaRecord;
import org.ipredencao.ipredencao_manager.jooq.tables.records.PessoaRelacionamentoRecord;
import org.ipredencao.ipredencao_manager.util.DateTimeHelper;
import org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil;
import org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo;
import org.ipredencao.ipredencao_manager.model.pessoa.PessoaQuery;
import org.ipredencao.ipredencao_manager.config.Roles;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.jooq.Condition;
import org.joda.time.DateTime;

@Repository
public class PessoaRepository {

    private static final org.ipredencao.ipredencao_manager.jooq.tables.Pessoa CHEFE = PESSOA.as("chefe");
    private static final org.ipredencao.ipredencao_manager.jooq.tables.Endereco END = ENDERECO.as("end");
    private static final org.ipredencao.ipredencao_manager.jooq.tables.Usuario USR = USUARIO.as("u");
    private static final org.ipredencao.ipredencao_manager.jooq.tables.Pessoa PP = PESSOA.as("pp");
    private static final org.ipredencao.ipredencao_manager.jooq.tables.Pessoa PR = PESSOA.as("pr");

    @Autowired
    private DSLContext dsl;

    @Autowired
    private EnderecoRepository enderecoRepository;
    
    public Pessoa insert(Pessoa pessoa) {
        PessoaRecord pessoaRecord = toRepository(pessoa);
        
        PessoaRecord saved = dsl.insertInto(PESSOA)
                .set(pessoaRecord)
                .returning()
                .fetchOne();

        return fromRepository(saved);
    }

    public Pessoa update(Pessoa pessoa) {
        PessoaRecord pessoaRecord = toRepository(pessoa);
        dsl.update(PESSOA)
            .set(pessoaRecord)
            .where(PESSOA.PESSOA_ID.eq(pessoa.getId()))
            .execute();

        return pessoa;
    }

    /**
     * Retorna a categoria_id em vigor para a pessoa imediatamente antes do timestamp informado.
     * <p>
     * O trigger {@code trigger_pessoa_history} grava NEW (estado após cada INSERT/UPDATE em pessoa)
     * em pessoa_history. Portanto, para descobrir o estado vigente antes de um ato oficial,
     * procuramos a entrada de histórico mais recente com {@code added_at} estritamente anterior
     * ao timestamp do ato. Usado para reverter pessoa.categoria_id ao deletar um ato.
     */
    public Optional<Long> findCategoriaIdBefore(Long pessoaId, DateTime threshold) {
        if (pessoaId == null || threshold == null) return Optional.empty();
        Long categoriaId = dsl.select(PESSOA_HISTORY.CATEGORIA_ID)
                .from(PESSOA_HISTORY)
                .where(PESSOA_HISTORY.PESSOA_ID.eq(pessoaId))
                .and(PESSOA_HISTORY.ADDED_AT.lessThan(DateTimeHelper.toDb(threshold)))
                .orderBy(PESSOA_HISTORY.ADDED_AT.desc(), PESSOA_HISTORY.HISTORY_ID.desc())
                .limit(1)
                .fetchOne(PESSOA_HISTORY.CATEGORIA_ID);
        return Optional.ofNullable(categoriaId);
    }

    public void deleteRelationship(Long pessoaId, Long pessoaRelacionadaId, TipoRelacionamento tipoRelacionamento) {
        PessoaRelacionamentoRecord record = findRelationshipRecord(pessoaId, pessoaRelacionadaId, tipoRelacionamento);
        if (record != null) {
            dsl.deleteFrom(PESSOA_RELACIONAMENTO)
                .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(record.getPessoaId())
                    .and(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(record.getPessoaRelacionadaId()))
                    .and(PESSOA_RELACIONAMENTO.TIPO_RELACIONAMENTO.eq(record.getTipoRelacionamento())))
                .execute();
        }
    }
    
    public void updateRelationship(Long pessoaId, Relacionamento existingRel, Relacionamento newRel) {
        PessoaRelacionamentoRecord record = findRelationshipRecord(pessoaId, existingRel.getPessoaRelacionadaId(), existingRel.getTipoRelacionamento());
        if (record != null) {
            dsl.update(PESSOA_RELACIONAMENTO)
                .set(PESSOA_RELACIONAMENTO.INICIO_RELACIONAMENTO, DateTimeHelper.toDb(newRel.getInicioRelacionamento()))
                .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(record.getPessoaId())
                    .and(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(record.getPessoaRelacionadaId()))
                    .and(PESSOA_RELACIONAMENTO.TIPO_RELACIONAMENTO.eq(record.getTipoRelacionamento())))
                .execute();
        }
    }

    public List<Pessoa> find(PessoaQuery query) {
        return find(query, ConfidentialAccess.CALLER);
    }

    public List<Pessoa> find(PessoaQuery query, ConfidentialAccess access) {
        boolean canSeeConfidential = ConfidentialRows.canSee(access);
        Condition finalCondition = buildFinalCondition(query, canSeeConfidential);

        Set<PessoaInclude> includes = query.getIncludes();
        boolean includeChefe = includes.contains(PessoaInclude.CHEFE_DE_FAMILIA);
        boolean includeEndereco = includes.contains(PessoaInclude.ENDERECO);

        List<SelectField<?>> extraFields = new ArrayList<>();
        if (includeChefe) extraFields.add(CHEFE.NOME);
        if (includeEndereco) {
            extraFields.addAll(List.of(
                END.ID, END.CEP, END.LOGRADOURO, END.NUMERO,
                END.COMPLEMENTO, END.BAIRRO, END.CIDADE, END.ESTADO,
                END.COORDENADAS, END.ADDED_AT, END.UPDATED_AT, END.UPDATED_BY));
        }

        var step = dsl.select(extraFields).select(PESSOA.asterisk()).from(PESSOA);
        if (includeChefe)
            step = step.leftJoin(CHEFE).on(PESSOA.CHEFE_DE_FAMILIA.eq(CHEFE.PESSOA_ID));
        if (includeEndereco)
            step = step.leftJoin(END).on(PESSOA.ENDERECO_ID.eq(END.ID));

        List<Record> records;
        if (query.getPagination() != null) {
            int limit = query.getPagination().getLimit() != null ? query.getPagination().getLimit() : Integer.MAX_VALUE;
            int offset = query.getPagination().getOffset() != null ? query.getPagination().getOffset() : 0;
            records = step.where(finalCondition).orderBy(PESSOA.NOME.asc()).limit(limit).offset(offset).fetch();
        } else {
            records = step.where(finalCondition).orderBy(PESSOA.NOME.asc()).fetch();
        }

        List<Pessoa> people = records.stream()
                .map(r -> fromRepository(r, includes))
                .toList();

        if (includeEndereco) {
            List<Endereco> addresses = people.stream()
                .map(Pessoa::getEndereco)
                .filter(Objects::nonNull)
                .toList();
            enderecoRepository.getPersonIdsFromAddresses(addresses);
        }

        if (includes.contains(PessoaInclude.RELACIONAMENTOS)) {
            loadRelationshipsForPeople(people, canSeeConfidential);
        }

        return people;
    }
    
    /**
     * Conta o total de pessoas que atendem aos critérios da query
     * (usado para paginação)
     */
    public long count(PessoaQuery query) {
        return count(query, ConfidentialAccess.CALLER);
    }

    public long count(PessoaQuery query, ConfidentialAccess access) {
        Condition finalCondition = buildFinalCondition(query, ConfidentialRows.canSee(access));
        return dsl.selectCount()
            .from(PESSOA)
            .where(finalCondition)
            .fetchOne(0, long.class);
    }

    public List<PessoaHistory> findHistoryByPersonId(Long pessoaId) {
        List<Record> records = dsl
            .select(PESSOA_HISTORY.asterisk(), USR.NAME)
            .from(PESSOA_HISTORY)
            .leftJoin(USR).on(PESSOA_HISTORY.UPDATED_BY.eq(USR.ID))
            .where(PESSOA_HISTORY.PESSOA_ID.eq(pessoaId))
            .orderBy(PESSOA_HISTORY.ADDED_AT.desc())
            .fetch();

        Map<Long, String> chefeDeFamiliaNomes = loadChefeDeFamiliaNomes(records);

        List<PessoaHistory> history = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            PessoaHistoryRecord currentRecord = records.get(i).into(PESSOA_HISTORY);
            PessoaHistoryRecord previousRecord = (i + 1 < records.size())
                ? records.get(i + 1).into(PESSOA_HISTORY) : null;

            List<PessoaHistoryChange> changes = PessoaHistoryDiff.compare(currentRecord, previousRecord, chefeDeFamiliaNomes);
            if (!changes.isEmpty()) {
                history.add(new PessoaHistory(
                    DateTimeHelper.fromDb(currentRecord.getAddedAt()),
                    currentRecord.getUpdatedBy(),
                    records.get(i).get(USR.NAME),
                    changes
                ));
            }
        }
        return history;
    }

    private Map<Long, String> loadChefeDeFamiliaNomes(List<Record> historyRecords) {
        Set<Long> ids = historyRecords.stream()
            .map(r -> r.get(PESSOA_HISTORY.CHEFE_DE_FAMILIA))
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet());

        if (ids.isEmpty()) return Map.of();

        return dsl
            .select(PESSOA.PESSOA_ID, PESSOA.NOME)
            .from(PESSOA)
            .where(PESSOA.PESSOA_ID.in(ids))
            .fetchMap(PESSOA.PESSOA_ID, PESSOA.NOME);
    }

    // CRUD para relacionamentos qualificados
    public Relacionamento insertRelationship(Long pessoaId, Relacionamento relacionamento) {
        PessoaRelacionamentoRecord relationamentoRecord = toRepository(relacionamento, pessoaId);
        PessoaRelacionamentoRecord saved = dsl.insertInto(PESSOA_RELACIONAMENTO)
                .set(relationamentoRecord)
                .returning()
                .fetchOne();
        return mapRelationship(saved, pessoaId);
    }
    
    /**
     * Busca um relacionamento existente considerando ambas as direções.
     * Para tipos simétricos (CONJUGE, NOIVO, etc), verifica A→B ou B→A.
     * Para tipos assimétricos (PAI/MAE ↔ FILHO), verifica o tipo complementar.
     * 
     * @param pessoaId ID da primeira pessoa
     * @param pessoaRelacionadaId ID da segunda pessoa
     * @param tipo Tipo do relacionamento
     * @return O relacionamento existente ou null se não encontrado
     */
    public Relacionamento findExistingRelationship(Long pessoaId, Long pessoaRelacionadaId, TipoRelacionamento tipo) {
        PessoaRelacionamentoRecord record = findRelationshipRecord(pessoaId, pessoaRelacionadaId, tipo);
        return record != null ? mapRelationship(record, pessoaId) : null;
    }
    
    /**
     * Método auxiliar que busca um record de relacionamento considerando ambas as direções.
     * Centraliza a lógica de busca usada por findExistingRelationship, deleteRelationship e updateRelationship.
     * 
     * @param pessoaId ID da primeira pessoa
     * @param pessoaRelacionadaId ID da segunda pessoa
     * @param tipo Tipo do relacionamento
     * @return O record do relacionamento ou null se não encontrado
     */
    private PessoaRelacionamentoRecord findRelationshipRecord(Long pessoaId, Long pessoaRelacionadaId, TipoRelacionamento tipo) {
        // Verificar se relacionamento existe na forma direta (pessoaId → pessoaRelacionadaId)
        PessoaRelacionamentoRecord directRecord = dsl.selectFrom(PESSOA_RELACIONAMENTO)
            .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(pessoaId)
                .and(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(pessoaRelacionadaId))
                .and(PESSOA_RELACIONAMENTO.TIPO_RELACIONAMENTO.eq(
                    org.ipredencao.ipredencao_manager.jooq.enums.TipoRelacionamento.valueOf(tipo.name()))))
            .fetchOne();
        
        if (directRecord != null) {
            return directRecord;
        }
        
        List<TipoRelacionamento> tiposInversos = TipoRelacionamento.getInverses(tipo);
        
        for (TipoRelacionamento tipoInverso : tiposInversos) {
            PessoaRelacionamentoRecord inverseRecord = dsl.selectFrom(PESSOA_RELACIONAMENTO)
                .where(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(pessoaRelacionadaId)
                    .and(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(pessoaId))
                    .and(PESSOA_RELACIONAMENTO.TIPO_RELACIONAMENTO.eq(
                        org.ipredencao.ipredencao_manager.jooq.enums.TipoRelacionamento.valueOf(tipoInverso.name()))))
                .fetchOne();
            
            if (inverseRecord != null) {
                return inverseRecord;
            }
        }
        
        return null;
    }
    
    /**
     * Mapeia um record de relacionamento para Relacionamento,
     * ajustando a perspectiva para a pessoa especificada
     */
    private Relacionamento mapRelationship(PessoaRelacionamentoRecord record, Long pessoaId) {
        Relacionamento rel = new Relacionamento();
        rel.setPessoaId(pessoaId);
        rel.setInicioRelacionamento(DateTimeHelper.fromDb(record.getInicioRelacionamento()));
        
        boolean pessoalPrincipal = record.getPessoaId().equals(pessoaId);
        
        if (pessoalPrincipal) {
            // Pessoa atual é a principal - usar dados diretos
            rel.setPessoaRelacionadaId(record.getPessoaRelacionadaId());
            if (record.getTipoRelacionamento() != null) {
                rel.setTipoRelacionamento(
                    TipoRelacionamento.valueOf(record.getTipoRelacionamento().name())
                );
            }
        } else {
            // Pessoa atual é a relacionada - inverter perspectiva
            rel.setPessoaRelacionadaId(record.getPessoaId()); // Outra pessoa
            if (record.getTipoRelacionamento() != null) {
                TipoRelacionamento tipoOriginal = TipoRelacionamento.valueOf(record.getTipoRelacionamento().name());
                Sexo sexo = find(PessoaQuery.builder().id(record.getPessoaId()).includes().build()).getFirst().getSexo();
                rel.setTipoRelacionamento(TipoRelacionamento.invert(tipoOriginal, sexo));
            }
        }
        
        // Buscar o nome da pessoa relacionada
        Long pessoaRelacionadaId = rel.getPessoaRelacionadaId();
        if (pessoaRelacionadaId != null) {
            // Sem o filtro, o retorno idempotente do POST entrega o nome do bebê em sigilo.
            Condition visivel = PESSOA.PESSOA_ID.eq(pessoaRelacionadaId);
            if (!ConfidentialRows.canSee(ConfidentialAccess.CALLER)) {
                visivel = visivel.and(ConfidentialRows.notConfidential(PESSOA.CATEGORIA_ID));
            }
            PessoaRecord pessoaRelacionada = dsl.selectFrom(PESSOA).where(visivel).fetchOne();
            if (pessoaRelacionada != null) {
                rel.setNomePessoaRelacionada(pessoaRelacionada.getNome());
            }
        }
        
        return rel;
    }
    
    /** Query própria: {@link #buildConditions} não a alcança, e o bebê em sigilo apareceria na ficha da mãe. */
    private void loadRelationshipsForPeople(List<Pessoa> people, boolean canSeeConfidential) {
        if (people.isEmpty()) return;

        List<Long> pessoaIds = people.stream().map(Pessoa::getId).toList();

        var records = dsl.select(
                PESSOA_RELACIONAMENTO.asterisk(),
                PP.NOME, PP.SEXO, PP.CATEGORIA_ID,
                PR.NOME, PR.SEXO, PR.CATEGORIA_ID)
            .from(PESSOA_RELACIONAMENTO)
            .leftJoin(PP).on(PESSOA_RELACIONAMENTO.PESSOA_ID.eq(PP.PESSOA_ID))
            .leftJoin(PR).on(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.eq(PR.PESSOA_ID))
            .where(PESSOA_RELACIONAMENTO.PESSOA_ID.in(pessoaIds)
                .or(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID.in(pessoaIds)))
            .fetch();

        Map<Long, List<Relacionamento>> relMap = new HashMap<>();
        for (var record : records) {
            Long principalId = record.get(PESSOA_RELACIONAMENTO.PESSOA_ID);
            Long relacionadaId = record.get(PESSOA_RELACIONAMENTO.PESSOA_RELACIONADA_ID);
            String nomePrincipal = record.get(PP.NOME);
            String nomeRelacionada = record.get(PR.NOME);
            var sexoPrincipal = record.get(PP.SEXO);
            var tipoDb = record.get(PESSOA_RELACIONAMENTO.TIPO_RELACIONAMENTO);

            // Cada ramo mostra a ponta oposta, então é a categoria dela que importa.
            boolean hidePrincipal = !canSeeConfidential && ConfidentialRows.isConfidential(record.get(PP.CATEGORIA_ID));
            boolean hideRelacionada = !canSeeConfidential && ConfidentialRows.isConfidential(record.get(PR.CATEGORIA_ID));

            if (pessoaIds.contains(principalId) && !hideRelacionada) {
                Relacionamento rel = new Relacionamento();
                rel.setPessoaId(principalId);
                rel.setPessoaRelacionadaId(relacionadaId);
                rel.setNomePessoaRelacionada(nomeRelacionada);
                rel.setInicioRelacionamento(DateTimeHelper.fromDb(record.get(PESSOA_RELACIONAMENTO.INICIO_RELACIONAMENTO)));
                if (tipoDb != null) rel.setTipoRelacionamento(TipoRelacionamento.valueOf(tipoDb.name()));
                relMap.computeIfAbsent(principalId, k -> new ArrayList<>()).add(rel);
            }

            if (pessoaIds.contains(relacionadaId) && !hidePrincipal) {
                Relacionamento rel = new Relacionamento();
                rel.setPessoaId(relacionadaId);
                rel.setPessoaRelacionadaId(principalId);
                rel.setNomePessoaRelacionada(nomePrincipal);
                rel.setInicioRelacionamento(DateTimeHelper.fromDb(record.get(PESSOA_RELACIONAMENTO.INICIO_RELACIONAMENTO)));
                if (tipoDb != null) {
                    TipoRelacionamento tipoOriginal = TipoRelacionamento.valueOf(tipoDb.name());
                    Sexo sexoModel = (sexoPrincipal != null) ? Sexo.valueOf(sexoPrincipal.name()) : null;
                    rel.setTipoRelacionamento(TipoRelacionamento.invert(tipoOriginal, sexoModel));
                }
                relMap.computeIfAbsent(relacionadaId, k -> new ArrayList<>()).add(rel);
            }
        }

        for (Pessoa p : people) {
            p.setRelacionamentos(relMap.getOrDefault(p.getId(), List.of()));
        }
    }

    private Condition buildFinalCondition(PessoaQuery query, boolean canSeeConfidential) {
        return QueryConditions.reduceToAnd(buildConditions(query, canSeeConfidential));
    }

    private List<Condition> buildConditions(PessoaQuery query, boolean canSeeConfidential) {
        List<Condition> conditions = new ArrayList<>();

        // Fecha find e count de uma vez; na busca por id o efeito é 404, não 403.
        if (!canSeeConfidential) conditions.add(ConfidentialRows.notConfidential(PESSOA.CATEGORIA_ID));
        if (query.getId() != null) conditions.add(PESSOA.PESSOA_ID.eq(query.getId()));
        if (query.getIds() != null && !query.getIds().isEmpty()) conditions.add(PESSOA.PESSOA_ID.in(query.getIds()));
        QueryConditions.addUnaccentedLike(conditions, PESSOA.NOME, query.getNome());
        QueryConditions.addUnaccentedLike(conditions, PESSOA.APELIDO, query.getApelido());
        QueryConditions.addEqIfNotBlank(conditions, PESSOA.EMAIL, query.getEmail());
        QueryConditions.addEqIfNotBlank(conditions, PESSOA.TELEFONE, query.getTelefone());
        QueryConditions.addEqIfNotBlank(conditions, PESSOA.CPF, query.getCpf());
        QueryConditions.addEqIfNotBlank(conditions, PESSOA.RG, query.getRg());
        QueryConditions.addEqIfNotBlank(conditions, PESSOA.CAMPUS, query.getCampus());
        if (query.getSexo() != null)
            conditions.add(PESSOA.SEXO.eq(org.ipredencao.ipredencao_manager.jooq.enums.Sexo.valueOf(query.getSexo().name())));
        if (query.getEstadoCivil() != null)
            conditions.add(PESSOA.ESTADO_CIVIL.eq(org.ipredencao.ipredencao_manager.jooq.enums.EstadoCivil.valueOf(query.getEstadoCivil().name())));
        if (query.getDataNascimentoFrom() != null)
            conditions.add(PESSOA.DATA_NASCIMENTO.greaterOrEqual(DateTimeHelper.toDb(query.getDataNascimentoFrom())));
        if (query.getDataNascimentoTo() != null)
            conditions.add(PESSOA.DATA_NASCIMENTO.lessOrEqual(DateTimeHelper.toDb(query.getDataNascimentoTo())));
        if (query.getTipoBatismo() != null)
            conditions.add(PESSOA.TIPO_BATISMO.eq(org.ipredencao.ipredencao_manager.jooq.enums.TipoBatismo.valueOf(query.getTipoBatismo().name())));
        if (query.getCategorias() != null && !query.getCategorias().isEmpty()) {
            List<Long> categoriaIds = query.getCategorias().stream()
                .map(CategoriaEnum::getId)
                .toList();
            conditions.add(PESSOA.CATEGORIA_ID.in(categoriaIds));
        }
        if (query.getEnderecoId() != null) conditions.add(PESSOA.ENDERECO_ID.eq(query.getEnderecoId()));
        // Pedir bookmark no corpo não muda nada abaixo de diácono.
        if (query.getBookmark() != null && SecurityUtils.hasAnyRole(Roles.staffNames())) {
            conditions.add(PESSOA.BOOKMARK.eq(query.getBookmark()));
        }
        return conditions;
    }

    private Pessoa fromRepository(Record record, Set<PessoaInclude> includes) {
        if (record == null) return null;
        PessoaRecord pessoaRecord = record.into(PESSOA);
        Pessoa p = mapPessoaFields(pessoaRecord);

        if (includes.contains(PessoaInclude.ENDERECO))
            p.setEndereco(extractEnderecoFromRecord(record));
        if (includes.contains(PessoaInclude.CHEFE_DE_FAMILIA))
            p.setChefeDeFamilia(extractChefeFromRecord(record, p.getChefeDeFamiliaId()));

        return p;
    }

    private Pessoa fromRepository(PessoaRecord pessoaRecord) {
        if (pessoaRecord == null) return null;
        return mapPessoaFields(pessoaRecord);
    }

    private Pessoa mapPessoaFields(PessoaRecord pessoaRecord) {
        Pessoa p = new Pessoa();
        p.setId(pessoaRecord.getPessoaId());
        p.setNome(pessoaRecord.getNome());
        p.setApelido(pessoaRecord.getApelido());
        p.setEmail(pessoaRecord.getEmail());
        p.setTelefone(pessoaRecord.getTelefone());
        p.setCampus(pessoaRecord.getCampus());
        p.setDataNascimento(DateTimeHelper.fromDb(pessoaRecord.getDataNascimento()));
        p.setDataFalecimento(DateTimeHelper.fromDb(pessoaRecord.getDataFalecimento()));
        p.setCpf(pessoaRecord.getCpf());
        p.setRg(pessoaRecord.getRg());
        if (pessoaRecord.getEstadoCivil() != null)
            p.setEstadoCivil(org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil.valueOf(pessoaRecord.getEstadoCivil().name()));
        p.setIgrejaAnterior(pessoaRecord.getIgrejaAnterior());
        p.setMotivosParaAdmissao(pessoaRecord.getMotivosParaAdmissao());
        if (pessoaRecord.getTipoBatismo() != null)
            p.setTipoBatismo(org.ipredencao.ipredencao_manager.model.pessoa.TipoBatismo.valueOf(pessoaRecord.getTipoBatismo().name()));
        p.setDataBatismo(DateTimeHelper.fromDb(pessoaRecord.getDataBatismo()));
        p.setDataProfissaoDeFe(DateTimeHelper.fromDb(pessoaRecord.getDataProfissaoDeFe()));
        p.setIgrejaBatismo(pessoaRecord.getIgrejaBatismo());
        if (pessoaRecord.getProfissao() != null && pessoaRecord.getProfissao().length > 0)
            p.setProfissao(Arrays.asList(pessoaRecord.getProfissao()));
        if (pessoaRecord.getEmpresa() != null && pessoaRecord.getEmpresa().length > 0)
            p.setEmpresa(Arrays.asList(pessoaRecord.getEmpresa()));
        p.setFotoUrl(pessoaRecord.getFotoUrl());
        if (pessoaRecord.getSexo() != null)
            p.setSexo(Sexo.valueOf(pessoaRecord.getSexo().name()));
        p.setChefeDeFamiliaId(pessoaRecord.getChefeDeFamilia());
        if (pessoaRecord.getCategoriaId() != null)
            p.setCategoria(CategoriaEnum.fromId(pessoaRecord.getCategoriaId()));
        if (pessoaRecord.getEmailsSecundarios() != null)
            p.setEmailsSecundarios(Arrays.asList(pessoaRecord.getEmailsSecundarios()));
        if (pessoaRecord.getTelefonesSecundarios() != null)
            p.setTelefonesSecundarios(Arrays.asList(pessoaRecord.getTelefonesSecundarios()));
        p.setUpdatedByUserId(pessoaRecord.getUpdatedBy());
        p.setBookmark(pessoaRecord.getBookmark());
        return p;
    }

    private Endereco extractEnderecoFromRecord(Record record) {
        Long endId = record.get(END.ID);
        if (endId == null) return null;
        Endereco e = new Endereco();
        e.setId(endId);
        e.setCep(record.get(END.CEP));
        e.setLogradouro(record.get(END.LOGRADOURO));
        e.setNumero(record.get(END.NUMERO));
        e.setComplemento(record.get(END.COMPLEMENTO));
        e.setBairro(record.get(END.BAIRRO));
        e.setCidade(record.get(END.CIDADE));
        e.setEstado(record.get(END.ESTADO));
        e.setCoordenadas(record.get(END.COORDENADAS));
        e.setAddedAt(DateTimeHelper.fromDb(record.get(END.ADDED_AT)));
        e.setUpdatedAt(DateTimeHelper.fromDb(record.get(END.UPDATED_AT)));
        e.setUpdatedByUserId(record.get(END.UPDATED_BY));
        return e;
    }

    private ChefeDeFamiliaRef extractChefeFromRecord(Record record, Long chefeId) {
        if (chefeId == null) return null;
        String chefeNome = record.get(CHEFE.NOME);
        if (chefeNome == null) return null;
        return new ChefeDeFamiliaRef(chefeId, chefeNome);
    }

    private static PessoaRecord toRepository(Pessoa pessoa) {
        PessoaRecord pessoaRecord = new PessoaRecord();
        pessoaRecord.setNome(pessoa.getNome());
        pessoaRecord.setApelido(pessoa.getApelido());
        pessoaRecord.setEmail(pessoa.getEmail());
        pessoaRecord.setTelefone(pessoa.getTelefone());
        pessoaRecord.setCampus(pessoa.getCampus());
        pessoaRecord.setDataNascimento(DateTimeHelper.toDb(pessoa.getDataNascimento()));
        pessoaRecord.setDataFalecimento(DateTimeHelper.toDb(pessoa.getDataFalecimento()));
        pessoaRecord.setCpf(pessoa.getCpf());
        pessoaRecord.setRg(pessoa.getRg());
        if (pessoa.getEstadoCivil() != null)
            pessoaRecord.setEstadoCivil(EstadoCivil.valueOf(pessoa.getEstadoCivil().name()));
        pessoaRecord.setIgrejaAnterior(pessoa.getIgrejaAnterior());
        pessoaRecord.setMotivosParaAdmissao(pessoa.getMotivosParaAdmissao());
        if (pessoa.getTipoBatismo() != null)
            pessoaRecord.setTipoBatismo(TipoBatismo.valueOf(pessoa.getTipoBatismo().name()));
        pessoaRecord.setDataBatismo(DateTimeHelper.toDb(pessoa.getDataBatismo()));
        pessoaRecord.setDataProfissaoDeFe(DateTimeHelper.toDb(pessoa.getDataProfissaoDeFe()));
        pessoaRecord.setIgrejaBatismo(pessoa.getIgrejaBatismo());
        
        // Converter List para arrays do PostgreSQL
        if (pessoa.getProfissao() != null && !pessoa.getProfissao().isEmpty()) {
            pessoaRecord.setProfissao(pessoa.getProfissao().toArray(new String[0]));
        }
        if (pessoa.getEmpresa() != null && !pessoa.getEmpresa().isEmpty()) {
            pessoaRecord.setEmpresa(pessoa.getEmpresa().toArray(new String[0]));
        }
        
        if (pessoa.getEndereco() != null)
            pessoaRecord.setEnderecoId(pessoa.getEndereco().getId());
        pessoaRecord.setFotoUrl(pessoa.getFotoUrl());
        if (pessoa.getSexo() != null)
            pessoaRecord.setSexo(org.ipredencao.ipredencao_manager.jooq.enums.Sexo.valueOf(pessoa.getSexo().name()));
        pessoaRecord.setChefeDeFamilia(pessoa.getChefeDeFamiliaId());
        if (pessoa.getCategoria() != null) {
            pessoaRecord.setCategoriaId(pessoa.getCategoria().getId());
        }
        
        // Converter List<String> para arrays do PostgreSQL
        if (pessoa.getEmailsSecundarios() != null && !pessoa.getEmailsSecundarios().isEmpty()) {
            pessoaRecord.setEmailsSecundarios(pessoa.getEmailsSecundarios().toArray(new String[0]));
        }
        if (pessoa.getTelefonesSecundarios() != null && !pessoa.getTelefonesSecundarios().isEmpty()) {
            pessoaRecord.setTelefonesSecundarios(pessoa.getTelefonesSecundarios().toArray(new String[0]));
        }
        pessoaRecord.setUpdatedBy(pessoa.getUpdatedByUserId());
        if (pessoa.getBookmark() != null) {
            pessoaRecord.setBookmark(pessoa.getBookmark());
        }
        
        return pessoaRecord;
    }

    private static PessoaRelacionamentoRecord toRepository(Relacionamento relacionamento, Long pessoaId) {
        PessoaRelacionamentoRecord record = new PessoaRelacionamentoRecord();

        record.setPessoaId(pessoaId);
        record.setPessoaRelacionadaId(relacionamento.getPessoaRelacionadaId());
        if (relacionamento.getTipoRelacionamento() != null)
            record.setTipoRelacionamento(
                org.ipredencao.ipredencao_manager.jooq.enums.TipoRelacionamento.valueOf(
                    relacionamento.getTipoRelacionamento().name()
                )
            );
        record.setInicioRelacionamento(DateTimeHelper.toDb(relacionamento.getInicioRelacionamento()));
        return record;
    }

    public void deletePregnancy(Long pessoaId) {
        dsl.deleteFrom(PESSOA_HISTORY).where(PESSOA_HISTORY.PESSOA_ID.eq(pessoaId)).execute();
        dsl.deleteFrom(PESSOA).where(PESSOA.PESSOA_ID.eq(pessoaId)).execute();
    }
} 