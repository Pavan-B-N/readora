package com.readora.catalog.repository;

import com.readora.catalog.entity.Book;
import com.readora.catalog.entity.VirtualEdition;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Composable filters for GET /books — Specification lets multiple optional query params combine
 * into one query rather than a growing if-chain of hand-written JPQL per filter combination.
 */
public final class BookSpecifications {

    private BookSpecifications() {
    }

    private static Predicate hasActiveVirtualEdition(Root<Book> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
        Subquery<UUID> subquery = criteriaQuery.subquery(UUID.class);
        Root<VirtualEdition> veRoot = subquery.from(VirtualEdition.class);
        subquery.select(veRoot.get("bookId")).where(cb.isTrue(veRoot.get("isActive")));
        return root.get("id").in(subquery);
    }

    /**
     * A book is "available to this customer" when it's either stocked at their store, or it has
     * an active virtual edition (store-independent, purchasable from anywhere). Shared by every
     * endpoint that surfaces books outside the main physical/virtual tab split — suggest and
     * recommendations both need this so they never point a customer at a book that isn't
     * actually deliverable to (or purchasable in) the store they're shopping.
     */
    private static Predicate availableToCustomer(Root<Book> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb, UUID storeId) {
        Predicate hasVirtualEdition = hasActiveVirtualEdition(root, criteriaQuery, cb);
        if (storeId == null) {
            // No resolved store yet (e.g. anonymous caller mid-load) — only the store-independent
            // virtual editions are safe to surface until we know where they're shopping.
            return hasVirtualEdition;
        }
        return cb.or(cb.equal(root.get("store").get("id"), storeId), hasVirtualEdition);
    }

    public static Specification<Book> withFilters(
            String query, UUID categoryId, UUID publisherId,
            BigDecimal minPrice, BigDecimal maxPrice, Boolean virtualOnly, UUID storeId, Set<UUID> ownedBookIds
    ) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isTrue(root.get("isActive")));

            if (Boolean.TRUE.equals(virtualOnly)) {
                predicates.add(hasActiveVirtualEdition(root, criteriaQuery, cb));
            } else if (Boolean.FALSE.equals(virtualOnly)) {
                // storeId is required and pre-validated by CatalogService.search whenever a
                // physical result could appear — "books actually stocked at the store the
                // customer is delivering from," never an unscoped cross-store view.
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            } else {
                // virtualOnly is null — the unified storefront view: physical-at-their-store or
                // virtual-anywhere, same rule as suggest/recommendations.
                predicates.add(availableToCustomer(root, criteriaQuery, cb, storeId));
            }

            if (ownedBookIds != null && !ownedBookIds.isEmpty()) {
                // A virtual-only book the caller already owns has nothing left to sell them here
                // — hide it from browsing entirely rather than showing a "buy again" listing in
                // the main catalogue (that's what the "Your orders" rail is for). Physical
                // listings are never affected: store IS NULL is false for every physical book
                // regardless of purchase history, so a book they own physically stays visible
                // (they may well want a second copy).
                predicates.add(cb.not(cb.and(cb.isNull(root.get("store")), root.get("id").in(ownedBookIds))));
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

    /** Typeahead suggest — title match, scoped to what's actually available to this customer. */
    public static Specification<Book> forSuggest(String query, UUID storeId) {
        return (root, criteriaQuery, cb) -> cb.and(
                cb.isTrue(root.get("isActive")),
                cb.like(cb.lower(root.get("title")), "%" + query.toLowerCase() + "%"),
                availableToCustomer(root, criteriaQuery, cb, storeId)
        );
    }

    /** Recommendations — same categories as past purchases, minus what's already owned, scoped to what's available to this customer. */
    public static Specification<Book> forRecommendations(Set<UUID> categoryIds, Set<UUID> excludeBookIds, UUID storeId) {
        return (root, criteriaQuery, cb) -> cb.and(
                cb.isTrue(root.get("isActive")),
                root.get("category").get("id").in(categoryIds),
                cb.not(root.get("id").in(excludeBookIds)),
                availableToCustomer(root, criteriaQuery, cb, storeId)
        );
    }
}
