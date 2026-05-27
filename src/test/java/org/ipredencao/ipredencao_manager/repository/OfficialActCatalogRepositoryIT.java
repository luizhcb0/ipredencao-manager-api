package org.ipredencao.ipredencao_manager.repository;

import org.ipredencao.ipredencao_manager.model.official_act.OfficialActForm;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActFormEnum;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActType;
import org.ipredencao.ipredencao_manager.model.official_act.OfficialActTypeEnum;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.jooq.Record;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ipredencao.ipredencao_manager.jooq.Tables.OFFICIAL_ACT_FORM;
import static org.ipredencao.ipredencao_manager.jooq.Tables.OFFICIAL_ACT_TYPE;

/** Verifies that the in-memory catalog matches the Liquibase seed and stays in sync with the Java enums. */
class OfficialActCatalogRepositoryIT extends IntegrationTestBase {

    @Autowired
    private OfficialActCatalogRepository catalog;

    @Test
    void getTypes_returnsAllFiveCanonicalTypesOrderedById() {
        List<OfficialActType> types = catalog.getTypes();

        assertThat(types)
                .hasSize(OfficialActTypeEnum.values().length)
                .extracting(OfficialActType::getId)
                .containsExactly(1L, 2L, 3L, 4L, 5L);

        for (OfficialActTypeEnum expected : OfficialActTypeEnum.values()) {
            OfficialActType actual = catalog.getTypeById(expected.getId());
            assertThat(actual.getCategory())
                    .as("category for type id %d", expected.getId())
                    .isEqualTo(expected.getCategory());
            assertThat(actual.getReferenceArticle())
                    .as("referenceArticle for type id %d", expected.getId())
                    .isEqualTo(expected.getReferenceArticle());
            assertThat(actual.getName()).isNotBlank();
        }
    }

    @Test
    void getTypes_eachTypeContainsItsForms() {
        for (OfficialActType type : catalog.getTypes()) {
            assertThat(type.getForms())
                    .as("forms for type id %d (%s)", type.getId(), type.getName())
                    .isNotNull()
                    .isNotEmpty()
                    .allSatisfy(form ->
                            assertThat(form.getOfficialActTypeId()).isEqualTo(type.getId()));
        }
    }

    @Test
    void getTypes_totalFormsAcrossAllTypesEqualsEnumLength() {
        long totalForms = catalog.getTypes().stream()
                .mapToLong(t -> t.getForms().size())
                .sum();

        assertThat(totalForms).isEqualTo(OfficialActFormEnum.values().length);
    }

    @Test
    void getFormById_resolvesEveryEnumIdToMatchingDatabaseRow() {
        for (OfficialActFormEnum expected : OfficialActFormEnum.values()) {
            OfficialActForm actual = catalog.getFormById(expected.getId());

            assertThat(actual)
                    .as("form for enum %s (id=%d)", expected, expected.getId())
                    .isNotNull();
            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getArticleClause()).isEqualTo(expected.getArticleClause());
            assertThat(actual.getOfficialActTypeId()).isEqualTo(expected.getType().getId());
            assertThat(actual.getName()).isNotBlank();
        }
    }

    @Test
    void getFormById_hydratesMetadataSchemaFromEnum() {
        for (OfficialActFormEnum expected : OfficialActFormEnum.values()) {
            OfficialActForm form = catalog.getFormById(expected.getId());

            assertThat(form.getMetadataSchema())
                    .as("metadataSchema for form %s", expected)
                    .containsExactlyElementsOf(expected.getMetadataFields());
        }
    }

    @Test
    void getFormById_unknownIdThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> catalog.getFormById(9_999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void getTypeById_unknownIdThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> catalog.getTypeById(9_999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void findFormById_unknownIdReturnsEmptyOptional() {
        Optional<OfficialActForm> result = catalog.findFormById(9_999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findFormById_knownIdReturnsPopulatedOptional() {
        OfficialActFormEnum sample = OfficialActFormEnum.ADM_MC_PROFISSAO_FE;

        Optional<OfficialActForm> result = catalog.findFormById(sample.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getArticleClause()).isEqualTo(sample.getArticleClause());
    }

    /** Drift sentinel between seed migration and {@link OfficialActTypeEnum}. */
    @Test
    void databaseTypesStaySynchronizedWithEnum() {
        Map<Long, Record> rowsById = dsl.select()
                .from(OFFICIAL_ACT_TYPE)
                .fetch()
                .stream()
                .collect(Collectors.toMap(r -> r.get(OFFICIAL_ACT_TYPE.ID), r -> r));

        assertThat(rowsById)
                .as("type rows in DB vs. OfficialActTypeEnum values")
                .hasSize(OfficialActTypeEnum.values().length);

        for (OfficialActTypeEnum expected : OfficialActTypeEnum.values()) {
            Record row = rowsById.get(expected.getId());
            assertThat(row)
                    .as("DB row for OfficialActTypeEnum.%s (id=%d)", expected, expected.getId())
                    .isNotNull();
            assertThat(row.get(OFFICIAL_ACT_TYPE.CATEGORY)).isEqualTo(expected.getCategory());
            assertThat(row.get(OFFICIAL_ACT_TYPE.REFERENCE_ARTICLE))
                    .isEqualTo(expected.getReferenceArticle());
        }
    }

    /** Drift sentinel between seed migration and {@link OfficialActFormEnum}. */
    @Test
    void databaseFormsStaySynchronizedWithEnum() {
        Map<Long, Record> rowsById = dsl.select()
                .from(OFFICIAL_ACT_FORM)
                .fetch()
                .stream()
                .collect(Collectors.toMap(r -> r.get(OFFICIAL_ACT_FORM.ID), r -> r));

        assertThat(rowsById)
                .as("form rows in DB vs. OfficialActFormEnum values")
                .hasSize(OfficialActFormEnum.values().length);

        for (OfficialActFormEnum expected : OfficialActFormEnum.values()) {
            Record row = rowsById.get(expected.getId());
            assertThat(row)
                    .as("DB row for OfficialActFormEnum.%s (id=%d)", expected, expected.getId())
                    .isNotNull();
            assertThat(row.get(OFFICIAL_ACT_FORM.OFFICIAL_ACT_TYPE_ID))
                    .isEqualTo(expected.getType().getId());
            assertThat(row.get(OFFICIAL_ACT_FORM.ARTICLE_CLAUSE))
                    .isEqualTo(expected.getArticleClause());
        }
    }
}
