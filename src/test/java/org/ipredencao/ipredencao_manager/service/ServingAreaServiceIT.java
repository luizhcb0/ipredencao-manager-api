package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.controller.form.ServingAreaForm;
import org.ipredencao.ipredencao_manager.controller.form.ServingAreaMemberForm;
import org.ipredencao.ipredencao_manager.controller.form.ServingAreaPositionForm;
import org.ipredencao.ipredencao_manager.controller.form.ServingAreaTeamForm;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.serving_area.ParticipationReportQuery;
import org.ipredencao.ipredencao_manager.model.serving_area.ParticipationReportRow;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingArea;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaMember;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPosition;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaPositionKindEnum;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaQuery;
import org.ipredencao.ipredencao_manager.model.serving_area.ServingAreaTeam;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.ipredencao.ipredencao_manager.support.ServingAreaFixture;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_MEMBER;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_MEMBER_HISTORY;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_POSITION;
import static org.ipredencao.ipredencao_manager.jooq.Tables.SERVING_AREA_TEAM;

@Transactional
class ServingAreaServiceIT extends IntegrationTestBase {

    @Autowired private ServingAreaService service;
    @Autowired private PessoaService pessoaService;

    private Pessoa person(String nome) {
        return PessoaFixture.membroComungante(pessoaService, nome, Sexo.MASCULINO);
    }

    @Test
    void create_createsThreeDefaultPositions() {
        ServingArea area = ServingAreaFixture.area(service, "Música");
        assertThat(area.positions()).extracting(ServingAreaPosition::kind)
                .containsExactlyInAnyOrder(ServingAreaPositionKindEnum.SUPERVISION,
                        ServingAreaPositionKindEnum.COORDINATION, ServingAreaPositionKindEnum.MEMBERSHIP);
    }

    @Test
    void seededAreas_existWithSingleMembershipCargo() {
        List<ServingArea> conselho = service.findPaginated(
                new ServingAreaQuery(null, "Conselho", null, null, null, null)).getData();
        assertThat(conselho).extracting(ServingArea::name).contains("Conselho");
        ServingArea detail = service.findDetail(conselho.get(0).id());
        assertThat(detail.positions()).hasSize(1);
        assertThat(detail.positions().get(0).kind()).isEqualTo(ServingAreaPositionKindEnum.MEMBERSHIP);
    }

    @Test
    void secondActiveSupervisor_rejected_thenAllowedAfterRemoval() {
        ServingArea area = ServingAreaFixture.area(service, "Louvor");
        Long supervisorPos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.SUPERVISION);
        Pessoa a = person("Presb A");
        Pessoa b = person("Presb B");

        ServingAreaMember first = ServingAreaFixture.addMember(service, area.id(), a.getId(), supervisorPos, null, null);

        assertThatThrownBy(() -> ServingAreaFixture.addMember(service, area.id(), b.getId(), supervisorPos, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("supervisor neste");

        service.deleteMember(area.id(), first.id());
        ServingAreaMember second = ServingAreaFixture.addMember(service, area.id(), b.getId(), supervisorPos, null, null);
        assertThat(second.id()).isNotNull();
    }

    @Test
    void supervisorWithTeam_succeeds() {
        ServingArea area = ServingAreaFixture.area(service, "Supervisão com equipe");
        Long supervisorPos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.SUPERVISION);
        ServingAreaTeam team = service.addTeam(area.id(), new ServingAreaTeamForm("Equipe A", null, null, true));
        Pessoa p = person("Supervisor Equipe");

        ServingAreaMember member = ServingAreaFixture.addMember(service, area.id(), p.getId(), supervisorPos, team.id(), null);

        assertThat(member.teamId()).isEqualTo(team.id());
        ServingArea listed = service.findPaginated(new ServingAreaQuery(area.id(), null, null, null, null, null))
                .getData().get(0);
        assertThat(listed.supervisorName()).isEqualTo(p.getNome());
    }

    @Test
    void coordinatorGeneral_andCoordinatorTeam_succeeds() {
        ServingArea area = ServingAreaFixture.area(service, "Coordenação mista");
        Long coordPos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.COORDINATION);
        ServingAreaTeam team = service.addTeam(area.id(), new ServingAreaTeamForm("Equipe B", null, null, true));
        Pessoa general = person("Coord Geral");
        Pessoa teamCoord = person("Coord Equipe");

        ServingAreaFixture.addMember(service, area.id(), general.getId(), coordPos, null, null);
        ServingAreaFixture.addMember(service, area.id(), teamCoord.getId(), coordPos, team.id(), null);

        ServingArea listed = service.findPaginated(new ServingAreaQuery(area.id(), null, null, null, null, null))
                .getData().get(0);
        assertThat(listed.coordinatorCount()).isEqualTo(2);
    }

    @Test
    void duplicateIdenticalMembership_rejected() {
        ServingArea area = ServingAreaFixture.area(service, "Mesa de som");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        Pessoa p = person("Operador");
        ServingAreaFixture.addMember(service, area.id(), p.getId(), pos, null, null);
        assertThatThrownBy(() -> ServingAreaFixture.addMember(service, area.id(), p.getId(), pos, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idêntico");
    }

    @Test
    void samePersonDifferentTeams_allowed() {
        ServingArea area = ServingAreaFixture.area(service, "Duas equipes");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        ServingAreaTeam t1 = service.addTeam(area.id(), new ServingAreaTeamForm("T1", null, null, true));
        ServingAreaTeam t2 = service.addTeam(area.id(), new ServingAreaTeamForm("T2", null, null, true));
        Pessoa p = person("Membro Duplo");

        ServingAreaFixture.addMember(service, area.id(), p.getId(), pos, t1.id(), null);
        ServingAreaMember second = ServingAreaFixture.addMember(service, area.id(), p.getId(), pos, t2.id(), null);
        assertThat(second.teamId()).isEqualTo(t2.id());
    }

    @Test
    void membershipWithoutTeam_whenNoTeams_succeeds() {
        ServingArea area = ServingAreaFixture.area(service, "Sem equipes");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        Pessoa p = person("Membro Geral");

        ServingAreaMember member = ServingAreaFixture.addMember(service, area.id(), p.getId(), pos, null, null);
        assertThat(member.teamId()).isNull();
    }

    @Test
    void membershipWithoutTeam_whenTeamsExist_rejected() {
        ServingArea area = ServingAreaFixture.area(service, "Com equipes");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        service.addTeam(area.id(), new ServingAreaTeamForm("Equipe", null, null, true));
        Pessoa p = person("Sem equipe");

        assertThatThrownBy(() -> ServingAreaFixture.addMember(service, area.id(), p.getId(), pos, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("equipe");
    }

    @Test
    void membershipWithTeam_whenTeamsExist_succeeds() {
        ServingArea area = ServingAreaFixture.area(service, "Membro em equipe");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        ServingAreaTeam team = service.addTeam(area.id(), new ServingAreaTeamForm("Equipe", null, null, true));
        Pessoa p = person("Membro Equipe");

        ServingAreaMember member = ServingAreaFixture.addMember(service, area.id(), p.getId(), pos, team.id(), null);
        assertThat(member.teamId()).isEqualTo(team.id());
    }

    @Test
    void updateMembership_clearTeam_whenTeamsExist_rejected() {
        ServingArea area = ServingAreaFixture.area(service, "Update sem equipe");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        ServingAreaTeam team = service.addTeam(area.id(), new ServingAreaTeamForm("Equipe", null, null, true));
        Pessoa p = person("Membro");
        ServingAreaMember member = ServingAreaFixture.addMember(service, area.id(), p.getId(), pos, team.id(), null);

        assertThatThrownBy(() -> service.updateMember(area.id(), member.id(),
                new ServingAreaMemberForm(p.getId(), pos, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("equipe");
    }

    @Test
    void positionKindChange_blockedWhenInUse() {
        ServingArea area = ServingAreaFixture.area(service, "Recepção");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        ServingAreaFixture.addMember(service, area.id(), person("Recepcionista").getId(), pos, null, null);
        assertThatThrownBy(() -> service.updatePosition(area.id(), pos,
                new ServingAreaPositionForm("Membro", ServingAreaPositionKindEnum.SUPERVISION, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tipo");
    }

    @Test
    void deletePositionInUse_blocked() {
        ServingArea area = ServingAreaFixture.area(service, "Infantil");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        ServingAreaFixture.addMember(service, area.id(), person("Tia").getId(), pos, null, null);
        assertThatThrownBy(() -> service.deletePosition(area.id(), pos))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("desative");
    }

    @Test
    void inactiveTeam_rejectedForNewMember() {
        ServingArea area = ServingAreaFixture.area(service, "Música com equipes");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        ServingAreaTeam team = service.addTeam(area.id(), new ServingAreaTeamForm("Violões", null, null, false));
        assertThatThrownBy(() -> ServingAreaFixture.addMember(service, area.id(), person("Violonista").getId(), pos, team.id(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inativa");
    }

    @Test
    void inactivePosition_rejectedForNewMember() {
        ServingArea area = ServingAreaFixture.area(service, "Cargo inativo");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        service.updatePosition(area.id(), pos,
                new ServingAreaPositionForm("Membro", ServingAreaPositionKindEnum.MEMBERSHIP, null, false));
        assertThatThrownBy(() -> ServingAreaFixture.addMember(service, area.id(), person("X").getId(), pos, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void listCounts_reflectMemberAndCoordinatorTotals() {
        ServingArea area = ServingAreaFixture.area(service, "Contagens");
        Long coordPos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.COORDINATION);
        Long memberPos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        ServingAreaTeam team = service.addTeam(area.id(), new ServingAreaTeamForm("Equipe", null, null, true));

        ServingAreaFixture.addMember(service, area.id(), person("Coord Geral").getId(), coordPos, null, null);
        ServingAreaFixture.addMember(service, area.id(), person("Coord Eq").getId(), coordPos, team.id(), null);
        ServingAreaFixture.addMember(service, area.id(), person("Membro Eq").getId(), memberPos, team.id(), null);

        ServingArea listed = service.findPaginated(new ServingAreaQuery(area.id(), null, null, null, null, null))
                .getData().get(0);
        assertThat(listed.teamCount()).isEqualTo(1);
        assertThat(listed.memberCount()).isEqualTo(1);
        assertThat(listed.coordinatorCount()).isEqualTo(2);
    }

    @Test
    void memberCount_excludesSupervisionAndCoordination() {
        ServingArea area = ServingAreaFixture.area(service, "Só membresia conta");
        Long supervisorPos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.SUPERVISION);
        Long coordPos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.COORDINATION);
        Long memberPos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);

        ServingAreaFixture.addMember(service, area.id(), person("Sup").getId(), supervisorPos, null, null);
        ServingAreaFixture.addMember(service, area.id(), person("Coord").getId(), coordPos, null, null);
        ServingAreaFixture.addMember(service, area.id(), person("Membro").getId(), memberPos, null, LocalDate.now());

        ServingArea listed = service.findPaginated(new ServingAreaQuery(area.id(), null, null, null, null, null))
                .getData().get(0);
        assertThat(listed.memberCount()).isEqualTo(1);
        assertThat(listed.coordinatorCount()).isEqualTo(1);
    }

    @Test
    void deleteMember_writesHistoryWithDeletedAt() {
        ServingArea area = ServingAreaFixture.area(service, "Histórico");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        ServingAreaMember member = ServingAreaFixture.addMember(service, area.id(), person("Ex-membro").getId(), pos, null, null);

        service.deleteMember(area.id(), member.id());

        assertThat(dsl.fetchCount(SERVING_AREA_MEMBER, SERVING_AREA_MEMBER.ID.eq(member.id()))).isZero();
        assertThat(dsl.fetchCount(SERVING_AREA_MEMBER_HISTORY,
                SERVING_AREA_MEMBER_HISTORY.PERSON_ID.eq(member.personId())
                        .and(SERVING_AREA_MEMBER_HISTORY.SERVING_AREA_ID.eq(area.id()))
                        .and(SERVING_AREA_MEMBER_HISTORY.DELETED_AT.isNotNull()))).isEqualTo(1);
    }

    @Test
    void deleteArea_cascadesPositionsTeamsMembers() {
        ServingArea area = ServingAreaFixture.area(service, "Efêmera");
        Long pos = ServingAreaFixture.positionId(area, ServingAreaPositionKindEnum.MEMBERSHIP);
        ServingAreaTeam team = service.addTeam(area.id(), new ServingAreaTeamForm("Equipe", null, null, true));
        ServingAreaFixture.addMember(service, area.id(), person("Membro").getId(), pos, team.id(), null);

        service.delete(area.id());

        assertThat(dsl.fetchCount(SERVING_AREA_MEMBER, SERVING_AREA_MEMBER.SERVING_AREA_ID.eq(area.id()))).isZero();
        assertThat(dsl.fetchCount(SERVING_AREA_POSITION, SERVING_AREA_POSITION.SERVING_AREA_ID.eq(area.id()))).isZero();
        assertThat(dsl.fetchCount(SERVING_AREA_TEAM, SERVING_AREA_TEAM.SERVING_AREA_ID.eq(area.id()))).isZero();
    }

    @Test
    void participationReport_serving_notServing_all_excludesDeceased_aggregates() {
        ServingArea m1 = ServingAreaFixture.area(service, "Serviço 1");
        ServingArea m2 = ServingAreaFixture.area(service, "Serviço 2");
        Long pos1 = ServingAreaFixture.positionId(m1, ServingAreaPositionKindEnum.MEMBERSHIP);
        Long pos2 = ServingAreaFixture.positionId(m2, ServingAreaPositionKindEnum.MEMBERSHIP);

        Pessoa serving = person("Serve em dois");
        ServingAreaFixture.addMember(service, m1.id(), serving.getId(), pos1, null, null);
        ServingAreaFixture.addMember(service, m2.id(), serving.getId(), pos2, null, null);
        Pessoa idle = person("Nao serve");
        Pessoa dead = person("Falecido");
        dead.setDataFalecimento(new DateTime(2020, 1, 1, 0, 0));
        pessoaService.update(dead);

        List<ParticipationReportRow> servingRows = service.participationReport(
                new ParticipationReportQuery(null, null, null, null, ParticipationReportQuery.Status.SERVING, null));
        ParticipationReportRow servingRow = servingRows.stream()
                .filter(r -> r.personId().equals(serving.getId())).findFirst().orElseThrow();
        assertThat(servingRow.memberships()).hasSize(2);
        assertThat(servingRows).noneMatch(r -> r.personId().equals(idle.getId()));

        List<ParticipationReportRow> notServing = service.participationReport(
                new ParticipationReportQuery(null, null, null, null, ParticipationReportQuery.Status.NOT_SERVING, null));
        assertThat(notServing).anyMatch(r -> r.personId().equals(idle.getId()));
        assertThat(notServing).noneMatch(r -> r.personId().equals(serving.getId()));
        assertThat(notServing).noneMatch(r -> r.personId().equals(dead.getId()));

        List<ParticipationReportRow> all = service.participationReport(
                new ParticipationReportQuery(null, null, null, null, ParticipationReportQuery.Status.ALL, null));
        assertThat(all).anyMatch(r -> r.personId().equals(serving.getId()));
        assertThat(all).anyMatch(r -> r.personId().equals(idle.getId()));
    }
}
