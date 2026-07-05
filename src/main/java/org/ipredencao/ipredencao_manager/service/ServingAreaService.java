package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.controller.form.ServingAreaForm;
import org.ipredencao.ipredencao_manager.controller.form.ServingAreaMemberForm;
import org.ipredencao.ipredencao_manager.controller.form.ServingAreaPositionForm;
import org.ipredencao.ipredencao_manager.controller.form.ServingAreaTeamForm;
import org.ipredencao.ipredencao_manager.model.pagination.PageInfo;
import org.ipredencao.ipredencao_manager.model.pagination.PagedResponse;
import org.ipredencao.ipredencao_manager.model.pagination.PaginationParameters;
import org.ipredencao.ipredencao_manager.model.serving_area.ParticipationReportQuery;
import org.ipredencao.ipredencao_manager.model.serving_area.ParticipationReportRow;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingArea;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaMember;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaMemberQuery;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPosition;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPositionKindEnum;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPositionQuery;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaQuery;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaTeam;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaTeamQuery;
import org.ipredencao.ipredencao_manager.repository.ServingAreaMemberRepository;
import org.ipredencao.ipredencao_manager.repository.ServingAreaPositionRepository;
import org.ipredencao.ipredencao_manager.repository.ServingAreaRepository;
import org.ipredencao.ipredencao_manager.repository.ServingAreaTeamRepository;
import org.ipredencao.ipredencao_manager.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ServingAreaService {

    @Autowired
    private ServingAreaRepository areaRepo;
    @Autowired
    private ServingAreaPositionRepository positionRepo;
    @Autowired
    private ServingAreaTeamRepository teamRepo;
    @Autowired
    private ServingAreaMemberRepository memberRepo;
    @Autowired
    private SecurityUtils securityUtils;

    // ===== Leitura =====

    @Transactional(readOnly = true)
    public PagedResponse<ServingArea> findPaginated(ServingAreaQuery query) {
        PaginationParameters pagination = query.pagination() != null ? query.pagination() : new PaginationParameters();
        pagination.applyDefaults();
        // ServingAreaQuery é imutável: reanexa a paginação resolvida para que o
        // repositório aplique limit/offset mesmo quando o body vem sem paginação.
        ServingAreaQuery effectiveQuery = ServingAreaQuery.builder()
                .id(query.id())
                .name(query.name())
                .active(query.active())
                .supervisorPersonId(query.supervisorPersonId())
                .personId(query.personId())
                .pagination(pagination)
                .build();
        List<ServingArea> areas = areaRepo.find(effectiveQuery);
        long total = areaRepo.count(effectiveQuery);
        return new PagedResponse<>(areas, new PageInfo(pagination.getLimit(), pagination.getOffset(), total));
    }

    @Transactional(readOnly = true)
    public ServingArea findDetail(Long id) {
        ServingArea area = requireArea(id);
        List<ServingAreaPosition> positions = positionRepo.find(
                ServingAreaPositionQuery.builder().servingAreaId(id).build());
        List<ServingAreaTeam> teams = teamRepo.find(
                ServingAreaTeamQuery.builder().servingAreaId(id).build());
        List<ServingAreaMember> members = memberRepo.find(
                ServingAreaMemberQuery.builder().servingAreaId(id).build());
        return area.withAggregates(positions, teams, members);
    }

    // Consulta genérica de vínculos (ficha da pessoa, relatório, etc.).
    @Transactional(readOnly = true)
    public List<ServingAreaMember> findMembers(ServingAreaMemberQuery query) {
        return memberRepo.find(query);
    }

    // ===== Serviço (área) =====

    @Transactional
    public ServingArea create(ServingAreaForm form) {
        validateName(form);
        validateWhatsapp(form.whatsappUrl());
        validateWhatsapp(form.coordinationWhatsappUrl());
        if (areaRepo.existsByName(form.name(), null)) {
            throw new IllegalArgumentException("Já existe um serviço com o nome \"" + form.name() + "\"");
        }
        Long userId = securityUtils.getCurrentUserId();
        boolean active = form.active() == null || form.active();
        ServingArea area = areaRepo.insert(form.name(), form.description(), form.whatsappUrl(),
                form.coordinationWhatsappUrl(), active, userId);

        // Cargos padrão de toda área criada pela aplicação.
        positionRepo.insert(area.id(), "Supervisor", ServingAreaPositionKindEnum.SUPERVISION, 0, true, userId);
        positionRepo.insert(area.id(), "Coordenador", ServingAreaPositionKindEnum.COORDINATION, 1, true, userId);
        positionRepo.insert(area.id(), "Membro", ServingAreaPositionKindEnum.MEMBERSHIP, 2, true, userId);

        return findDetail(area.id());
    }

    @Transactional
    public ServingArea update(Long id, ServingAreaForm form) {
        ServingArea existing = requireArea(id);
        validateName(form);
        validateWhatsapp(form.whatsappUrl());
        validateWhatsapp(form.coordinationWhatsappUrl());
        if (areaRepo.existsByName(form.name(), id)) {
            throw new IllegalArgumentException("Já existe um serviço com o nome \"" + form.name() + "\"");
        }
        Long userId = securityUtils.getCurrentUserId();
        boolean active = form.active() != null ? form.active() : existing.active();
        areaRepo.update(id, form.name(), form.description(), form.whatsappUrl(),
                form.coordinationWhatsappUrl(), active, userId);
        return findDetail(id);
    }

    @Transactional
    public void delete(Long id) {
        requireArea(id);
        areaRepo.delete(id); // cascade apaga cargos + equipes + membros
    }

    // ===== Cargos =====

    @Transactional
    public ServingAreaPosition addPosition(Long areaId, ServingAreaPositionForm form) {
        requireArea(areaId);
        if (form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("Nome do cargo é obrigatório");
        }
        if (form.kind() == null) {
            throw new IllegalArgumentException("Tipo do cargo (kind) é obrigatório");
        }
        checkPositionNameUnique(areaId, form.name(), null);
        Long userId = securityUtils.getCurrentUserId();
        int sortOrder = form.sortOrder() != null ? form.sortOrder()
                : positionRepo.find(ServingAreaPositionQuery.builder().servingAreaId(areaId).build()).size();
        boolean active = form.active() == null || form.active();
        return positionRepo.insert(areaId, form.name(), form.kind(), sortOrder, active, userId);
    }

    @Transactional
    public ServingAreaPosition updatePosition(Long areaId, Long positionId, ServingAreaPositionForm form) {
        ServingAreaPosition existing = requirePosition(areaId, positionId);
        if (form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("Nome do cargo é obrigatório");
        }
        ServingAreaPositionKindEnum kind = form.kind() != null ? form.kind() : existing.kind();
        if (kind != existing.kind() && positionRepo.hasMembers(positionId)) {
            throw new IllegalStateException("Não é possível mudar o tipo de um cargo que já possui vínculos");
        }
        checkPositionNameUnique(areaId, form.name(), positionId);
        Long userId = securityUtils.getCurrentUserId();
        int sortOrder = form.sortOrder() != null ? form.sortOrder() : existing.sortOrder();
        boolean active = form.active() != null ? form.active() : existing.active();
        return positionRepo.update(positionId, areaId, form.name(), kind, sortOrder, active, userId);
    }

    @Transactional
    public void deletePosition(Long areaId, Long positionId) {
        requirePosition(areaId, positionId);
        if (positionRepo.hasMembers(positionId)) {
            throw new IllegalStateException("Cargo possui vínculos e não pode ser excluído; desative-o em vez de excluir");
        }
        positionRepo.delete(positionId);
    }

    // ===== Equipes =====

    @Transactional
    public ServingAreaTeam addTeam(Long areaId, ServingAreaTeamForm form) {
        requireArea(areaId);
        if (form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("Nome da equipe é obrigatório");
        }
        validateWhatsapp(form.whatsappUrl());
        checkTeamNameUnique(areaId, form.name(), null);
        Long userId = securityUtils.getCurrentUserId();
        boolean active = form.active() == null || form.active();
        return teamRepo.insert(areaId, form.name(), form.description(), form.whatsappUrl(), active, userId);
    }

    @Transactional
    public ServingAreaTeam updateTeam(Long areaId, Long teamId, ServingAreaTeamForm form) {
        ServingAreaTeam existing = requireTeam(areaId, teamId);
        if (form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("Nome da equipe é obrigatório");
        }
        validateWhatsapp(form.whatsappUrl());
        checkTeamNameUnique(areaId, form.name(), teamId);
        Long userId = securityUtils.getCurrentUserId();
        boolean active = form.active() != null ? form.active() : existing.active();
        return teamRepo.update(teamId, areaId, form.name(), form.description(), form.whatsappUrl(), active, userId);
    }

    @Transactional
    public void deleteTeam(Long areaId, Long teamId) {
        requireTeam(areaId, teamId);
        if (teamRepo.hasMembers(teamId)) {
            throw new IllegalStateException("Equipe possui vínculos e não pode ser excluída; desative-a em vez de excluir");
        }
        teamRepo.delete(teamId);
    }

    // ===== Vínculos (membros) =====

    @Transactional
    public ServingAreaMember addMember(Long areaId, ServingAreaMemberForm form) {
        requireArea(areaId);
        if (form.personId() == null) {
            throw new IllegalArgumentException("Pessoa (personId) é obrigatória");
        }
        ServingAreaPosition position = validatePositionForMember(areaId, form.positionId(), true);
        validateSupervisionScope(position, form.teamId());
        validateTeamForMember(areaId, form.teamId(), true);
        validateMembershipTeamRequirement(areaId, position, form.teamId());
        if (memberRepo.existsDuplicate(areaId, form.personId(), form.positionId(), form.teamId(), null)) {
            throw new IllegalArgumentException("Já existe um vínculo idêntico para esta pessoa");
        }
        checkSingleSupervisor(areaId, position, null);
        Long userId = securityUtils.getCurrentUserId();
        Long id = memberRepo.insert(areaId, form.personId(), form.positionId(), form.teamId(),
                form.startDate(), userId);
        return requireMember(areaId, id);
    }

    @Transactional
    public ServingAreaMember updateMember(Long areaId, Long memberId, ServingAreaMemberForm form) {
        requireMember(areaId, memberId);
        if (form.personId() == null) {
            throw new IllegalArgumentException("Pessoa (personId) é obrigatória");
        }
        ServingAreaPosition position = validatePositionForMember(areaId, form.positionId(), false);
        validateSupervisionScope(position, form.teamId());
        validateTeamForMember(areaId, form.teamId(), false);
        validateMembershipTeamRequirement(areaId, position, form.teamId());
        if (memberRepo.existsDuplicate(areaId, form.personId(), form.positionId(), form.teamId(), memberId)) {
            throw new IllegalArgumentException("Já existe um vínculo idêntico para esta pessoa");
        }
        checkSingleSupervisor(areaId, position, memberId);
        Long userId = securityUtils.getCurrentUserId();
        memberRepo.update(memberId, areaId, form.personId(), form.positionId(), form.teamId(),
                form.startDate(), userId);
        return requireMember(areaId, memberId);
    }

    @Transactional
    public void deleteMember(Long areaId, Long memberId) {
        requireMember(areaId, memberId);
        memberRepo.delete(memberId);
    }

    // ===== Relatório de participação =====

    @Transactional(readOnly = true)
    public List<ParticipationReportRow> participationReport(ParticipationReportQuery query) {
        ParticipationReportQuery.Status status = query.status() != null
                ? query.status() : ParticipationReportQuery.Status.SERVING;
        boolean includeDeceased = query.includeDeceased() != null && query.includeDeceased();

        List<ServingAreaMember> currentMembers = memberRepo.find(ServingAreaMemberQuery.builder()

                .servingAreaId(query.servingAreaId())
                .positionId(query.positionId())
                .kind(query.kind())
                .categoryIds(query.categoryIds())
                .includeDeceased(includeDeceased)
                .build());

        Map<Long, ParticipationReportRow> byPerson = new LinkedHashMap<>();
        for (ServingAreaMember m : currentMembers) {
            ParticipationReportRow row = byPerson.computeIfAbsent(m.personId(), k -> new ParticipationReportRow(
                    m.personId(), m.personName(), m.personCategoryId(), m.personCategoryName(), new ArrayList<>()));
            row.memberships().add(new ParticipationReportRow.Membership(
                    m.servingAreaId(), m.servingAreaName(), m.positionName(), m.kind(), m.teamName(),
                    m.startDate()));
        }
        List<ParticipationReportRow> serving = new ArrayList<>(byPerson.values());
        if (status == ParticipationReportQuery.Status.SERVING) {
            return serving;
        }

        List<ParticipationReportRow> people = memberRepo.findReportPeople(query.categoryIds(), includeDeceased);
        List<ParticipationReportRow> notServing = people.stream()
                .filter(p -> !byPerson.containsKey(p.personId()))
                .toList();
        if (status == ParticipationReportQuery.Status.NOT_SERVING) {
            return notServing;
        }

        List<ParticipationReportRow> all = new ArrayList<>(serving);
        all.addAll(notServing);
        all.sort((a, b) -> {
            String an = a.personName() != null ? a.personName() : "";
            String bn = b.personName() != null ? b.personName() : "";
            return an.compareToIgnoreCase(bn);
        });
        return all;
    }

    // ===== Validações / helpers =====

    private ServingArea requireArea(Long id) {
        return areaRepo.find(ServingAreaQuery.builder().id(id).build()).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Serviço " + id + " não encontrado"));
    }

    private ServingAreaPosition requirePosition(Long areaId, Long positionId) {
        return positionRepo.find(ServingAreaPositionQuery.builder().id(positionId).servingAreaId(areaId).build())
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cargo " + positionId + " não encontrado neste serviço"));
    }

    private ServingAreaTeam requireTeam(Long areaId, Long teamId) {
        return teamRepo.find(ServingAreaTeamQuery.builder().id(teamId).servingAreaId(areaId).build())
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Equipe " + teamId + " não encontrada neste serviço"));
    }

    private ServingAreaMember requireMember(Long areaId, Long memberId) {
        return memberRepo.find(ServingAreaMemberQuery.builder().id(memberId).servingAreaId(areaId).build())
                .stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException("Vínculo " + memberId + " não encontrado neste serviço"));
    }

    private ServingAreaPosition validatePositionForMember(Long areaId, Long positionId, boolean requireActive) {
        if (positionId == null) throw new IllegalArgumentException("Cargo (positionId) é obrigatório");
        ServingAreaPosition position = requirePosition(areaId, positionId);
        if (requireActive && Boolean.FALSE.equals(position.active())) {
            throw new IllegalArgumentException("Cargo está inativo e não pode receber novos vínculos");
        }
        return position;
    }

    private void validateTeamForMember(Long areaId, Long teamId, boolean requireActive) {
        if (teamId == null) return; // escopo geral da área
        ServingAreaTeam team = requireTeam(areaId, teamId);
        if (requireActive && Boolean.FALSE.equals(team.active())) {
            throw new IllegalArgumentException("Equipe está inativa e não pode receber novos vínculos");
        }
    }

    // A supervisão é sempre do serviço inteiro; nunca fica vinculada a uma equipe.
    private void validateSupervisionScope(ServingAreaPosition position, Long teamId) {
        if (position.kind() == ServingAreaPositionKindEnum.SUPERVISION && teamId != null) {
            throw new IllegalArgumentException("A supervisão é do serviço inteiro e não pode ser vinculada a uma equipe");
        }
    }

    // Com equipes cadastradas, vínculo de membresia exige teamId (supervisão/coordenação
    // podem permanecer no escopo geral ou de equipe).
    private void validateMembershipTeamRequirement(Long areaId, ServingAreaPosition position, Long teamId) {
        if (position.kind() != ServingAreaPositionKindEnum.MEMBERSHIP) return;
        // Só equipes ativas contam: se todas estiverem inativas, o escopo geral é
        // permitido (senão a membresia ficaria impossível de cadastrar).
        boolean hasActiveTeams = !teamRepo.find(
                ServingAreaTeamQuery.builder().servingAreaId(areaId).active(true).build()).isEmpty();
        if (hasActiveTeams && teamId == null) {
            throw new IllegalArgumentException("Selecione uma equipe para o vínculo de membresia");
        }
    }

    // No máximo 1 supervisor por serviço; a troca é manual (remova o atual
    // antes de definir outro).
    private void checkSingleSupervisor(Long areaId, ServingAreaPosition position, Long excludeMemberId) {
        if (position.kind() != ServingAreaPositionKindEnum.SUPERVISION) return;
        boolean otherSupervisor = memberRepo.find(ServingAreaMemberQuery.builder()
                        .servingAreaId(areaId)
                        .kind(ServingAreaPositionKindEnum.SUPERVISION)
                        .build()).stream()
                .anyMatch(m -> excludeMemberId == null || !m.id().equals(excludeMemberId));
        if (otherSupervisor) {
            throw new IllegalStateException("Já existe um supervisor neste serviço; remova-o antes de definir outro");
        }
    }

    // Espelha a UNIQUE(serving_area_id, name) do banco para devolver 400 em PT em
    // vez de deixar a violação de constraint virar 500 genérico.
    private void checkPositionNameUnique(Long areaId, String name, Long excludeId) {
        boolean duplicate = positionRepo.find(ServingAreaPositionQuery.builder().servingAreaId(areaId).build()).stream()
                .anyMatch(p -> p.name().equals(name) && !p.id().equals(excludeId));
        if (duplicate) {
            throw new IllegalArgumentException("Já existe um cargo com o nome \"" + name + "\" neste serviço");
        }
    }

    private void checkTeamNameUnique(Long areaId, String name, Long excludeId) {
        boolean duplicate = teamRepo.find(ServingAreaTeamQuery.builder().servingAreaId(areaId).build()).stream()
                .anyMatch(t -> t.name().equals(name) && !t.id().equals(excludeId));
        if (duplicate) {
            throw new IllegalArgumentException("Já existe uma equipe com o nome \"" + name + "\" neste serviço");
        }
    }

    private void validateName(ServingAreaForm form) {
        if (form == null || form.name() == null || form.name().isBlank()) {
            throw new IllegalArgumentException("Nome do serviço é obrigatório");
        }
    }

    private void validateWhatsapp(String url) {
        if (url != null && !url.isBlank() && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Link de WhatsApp deve começar com https://");
        }
    }
}
