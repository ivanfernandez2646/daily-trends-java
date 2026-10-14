package com.ivanfernandez.dailytrends.contexts.cms.shared.domain.criteria;

/** The condition "field equals value". */
public record Filter<F>(F field, Object value) {}
