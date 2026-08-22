package com.readora.catalog.repository;

import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.BookFormat;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Composable filters for GET /books — Specification lets multiple optional query params combine
 * into one query rather than a growing if-chain of hand-written JPQL per filter combination.
 */
public final class BookSpecifications {

    private BookSpecifications() {
    }

    public static Specification<Book> withFilters(
            String query, UUID categoryId, UUID publisherId, BookFormat format,
            BigDecimal minPrice, BigDecimal maxPrice
    ) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));

            if (query != null && !query.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + query.toLowerCase() + "%"));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (publisherId != null) {
                predicates.add(cb.equal(root.get("publisher").get("id"), publisherId));
            }
            if (format != null) {
                predicates.add(cb.equal(root.get("format"), format));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("listPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("listPrice"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
