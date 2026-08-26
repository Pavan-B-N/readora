package com.readora.catalog.repository;

import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.VirtualEdition;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
            String query, UUID categoryId, UUID publisherId,
            BigDecimal minPrice, BigDecimal maxPrice, boolean virtualOnly, UUID storeId
    ) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));

            if (virtualOnly) {
                Subquery<UUID> hasActiveVirtualEdition = criteriaQuery.subquery(UUID.class);
                Root<VirtualEdition> veRoot = hasActiveVirtualEdition.from(VirtualEdition.class);
                hasActiveVirtualEdition.select(veRoot.get("bookId")).where(cb.isTrue(veRoot.get("isActive")));
                predicates.add(root.get("id").in(hasActiveVirtualEdition));
            } else {
                predicates.add(cb.isNotNull(root.get("store")));
                // Virtual editions are store-independent by design (see the branch above), so
                // this only scopes the physical tab — "books actually stocked at the store the
                // customer is delivering from," not just "any physical book anywhere."
                if (storeId != null) {
                    predicates.add(cb.equal(root.get("store").get("id"), storeId));
                }
            }

            if (query != null && !query.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + query.toLowerCase() + "%"));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (publisherId != null) {
                predicates.add(cb.equal(root.get("publisher").get("id"), publisherId));
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
