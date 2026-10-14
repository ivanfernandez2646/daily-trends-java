package com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CriteriaTest {

    private enum Field {
        NAME,
        AGE
    }

    @Test
    void all_has_no_filters_order_or_limit() {
        Criteria<Field> criteria = Criteria.all();

        assertThat(criteria.filters()).isEmpty();
        assertThat(criteria.order()).isEmpty();
        assertThat(criteria.limit()).isEmpty();
    }

    @Test
    void keeps_every_filter_to_combine_them_with_or() {
        var filters = List.of(new Filter<>(Field.NAME, "a"), new Filter<>(Field.NAME, "b"));

        Criteria<Field> criteria = Criteria.withFilters(filters);

        assertThat(criteria.filters()).containsExactlyElementsOf(filters);
    }

    @Test
    void keeps_the_order() {
        Criteria<Field> criteria = Criteria.<Field>all().orderBy(Field.AGE, Direction.DESC);

        assertThat(criteria.order()).contains(new Order<>(Field.AGE, Direction.DESC));
    }

    @Test
    void keeps_a_positive_limit() {
        Criteria<Field> criteria = Criteria.<Field>all().limit(10);

        assertThat(criteria.limit()).hasValue(10);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejects_a_limit_that_is_not_positive(int limit) {
        assertThatThrownBy(() -> Criteria.<Field>all().limit(limit))
                .isInstanceOf(InvalidArgumentError.class)
                .hasMessage("<Criteria> does not allow the limit <" + limit + ">");
    }
}
