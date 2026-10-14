package com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria;

import com.ivanfernandez.dailytrends.contexts.cms.shared.domain.InvalidArgumentError;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * A search over the fields {@code F} of an aggregate. Filters are combined with OR; no filters means
 * every element. Criteria are built by code, never from a request.
 */
public record Criteria<F>(List<Filter<F>> filters, Optional<Order<F>> order, OptionalInt limit) {

    public Criteria {
        filters = List.copyOf(filters);
        if (limit.isPresent() && limit.getAsInt() <= 0) {
            throw new InvalidArgumentError("<Criteria> does not allow the limit <" + limit.getAsInt() + ">");
        }
    }

    public static <F> Criteria<F> all() {
        return new Criteria<>(List.of(), Optional.empty(), OptionalInt.empty());
    }

    public static <F> Criteria<F> withFilters(List<Filter<F>> filters) {
        return new Criteria<>(filters, Optional.empty(), OptionalInt.empty());
    }

    public Criteria<F> orderBy(F field, Direction direction) {
        return new Criteria<>(filters, Optional.of(new Order<>(field, direction)), limit);
    }

    public Criteria<F> limit(int limit) {
        return new Criteria<>(filters, order, OptionalInt.of(limit));
    }
}
