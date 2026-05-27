package org.ipredencao.ipredencao_manager.service;

import org.ipredencao.ipredencao_manager.model.official_act.OfficialAct;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActType;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.CategorySection;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.FormGroup;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.MinuteReportLine;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.MinuteReportResponse;
import org.ipredencao.ipredencao_manager.model.official_act.minute_report.TypeGroup;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.repository.OfficialActCatalogRepository;
import org.ipredencao.ipredencao_manager.repository.OfficialActRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monta o relatório de uma ata em hierarquia categoria → tipo → forma → linhas.
 * O texto de cada linha é gerado pelo {@link MinuteReportFormatter}.
 */
@Service
public class MinuteReportService {

    private static final Map<String, String> CATEGORY_TITLES = Map.of(
            "ADMISSAO", "Admissões",
            "DEMISSAO", "Demissões");

    private static final List<String> CATEGORY_ORDER = List.of("ADMISSAO", "DEMISSAO");

    @Autowired
    private OfficialActRepository repo;
    @Autowired
    private OfficialActCatalogRepository catalog;
    @Autowired
    private PessoaService pessoaService;
    @Autowired
    private MinuteReportFormatter formatter;

    public MinuteReportResponse generate(String minuteNumber) {
        if (minuteNumber == null || minuteNumber.isBlank()) {
            throw new IllegalArgumentException("minuteNumber é obrigatório");
        }
        List<OfficialAct> acts = repo.findByMinuteNumber(minuteNumber);
        if (acts.isEmpty()) {
            throw new NoSuchElementException("Nenhum ato encontrado para a ata " + minuteNumber);
        }

        // Batch: uma única query IN (?) carrega todas as pessoas de uma vez.
        Set<Long> personIds = acts.stream()
                .map(OfficialAct::getPersonId)
                .collect(Collectors.toSet());
        Map<Long, Pessoa> peopleById = pessoaService.findByIds(personIds).stream()
                .collect(Collectors.toMap(Pessoa::getId, Function.identity()));

        Map<String, Map<Long, Map<Long, List<MinuteReportLine>>>> hierarchy = buildHierarchy(acts, peopleById);
        List<CategorySection> sections = renderSections(hierarchy);

        return new MinuteReportResponse(minuteNumber, acts.get(0).getMinuteDate(), sections);
    }

    // ===== Agrupamento =====

    private Map<String, Map<Long, Map<Long, List<MinuteReportLine>>>> buildHierarchy(
            List<OfficialAct> acts, Map<Long, Pessoa> peopleById) {

        Map<String, Map<Long, Map<Long, List<MinuteReportLine>>>> hierarchy = new LinkedHashMap<>();
        for (OfficialAct act : acts) {
            String category = act.getCategory();
            Long typeId = act.getOfficialActTypeId();
            Long formId = act.getOfficialActFormId();

            MinuteReportLine line = new MinuteReportLine(
                    act.getId(),
                    act.getPersonId(),
                    act.getAdmissionOrderNumber(),
                    formatter.format(act, peopleById.get(act.getPersonId()))
            );

            hierarchy
                    .computeIfAbsent(category, k -> new LinkedHashMap<>())
                    .computeIfAbsent(typeId, k -> new LinkedHashMap<>())
                    .computeIfAbsent(formId, k -> new ArrayList<>())
                    .add(line);
        }
        return hierarchy;
    }

    private List<CategorySection> renderSections(
            Map<String, Map<Long, Map<Long, List<MinuteReportLine>>>> hierarchy) {

        List<CategorySection> sections = new ArrayList<>();
        for (String category : CATEGORY_ORDER) {
            Map<Long, Map<Long, List<MinuteReportLine>>> byType = hierarchy.get(category);
            if (byType == null || byType.isEmpty()) continue;

            List<TypeGroup> typeGroups = new ArrayList<>();
            for (Map.Entry<Long, Map<Long, List<MinuteReportLine>>> typeEntry : byType.entrySet()) {
                OfficialActType type = catalog.getTypeById(typeEntry.getKey());
                List<FormGroup> formGroups = new ArrayList<>();
                for (Map.Entry<Long, List<MinuteReportLine>> formEntry : typeEntry.getValue().entrySet()) {
                    var form = catalog.getFormById(formEntry.getKey());
                    formGroups.add(new FormGroup(
                            form.getId(),
                            form.getName(),
                            form.getArticleClause(),
                            formEntry.getValue()));
                }
                typeGroups.add(new TypeGroup(
                        type.getId(),
                        type.getName(),
                        type.getReferenceArticle(),
                        formGroups));
            }
            sections.add(new CategorySection(
                    category,
                    CATEGORY_TITLES.getOrDefault(category, category),
                    typeGroups));
        }
        return sections;
    }
}
