package org.ipredencao.ipredencao_manager.model.serving_area;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.joda.time.DateTime;

import java.util.List;

// Resposta de leitura. supervisor* e contagens derivadas vêm de subconsultas na
// listagem; positions/teams/members só vêm preenchidos no detalhe.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServingArea(
        Long id,
        String name,
        String description,
        String whatsappUrl,
        String coordinationWhatsappUrl,
        Boolean active,
        DateTime addedAt,
        DateTime updatedAt,
        Long updatedBy,
        Long supervisorPersonId,
        String supervisorName,
        Integer teamCount,
        Integer memberCount,
        Integer coordinatorCount,
        List<ServingAreaPosition> positions,
        List<ServingAreaTeam> teams,
        List<ServingAreaMember> members
) {
    public ServingArea withAggregates(
            List<ServingAreaPosition> positions,
            List<ServingAreaTeam> teams,
            List<ServingAreaMember> members) {
        return new ServingArea(
                id, name, description, whatsappUrl, coordinationWhatsappUrl, active,
                addedAt, updatedAt, updatedBy,
                supervisorPersonId, supervisorName,
                teamCount, memberCount, coordinatorCount,
                positions, teams, members);
    }
}
