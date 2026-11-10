package com.example.graphqldemo.product;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Builds the WHERE clause dynamically from whichever filter fields the caller supplied. */
public final class ProductSpecs {

    private ProductSpecs() {}

    public static Specification<Product> from(ProductFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (f.category() != null && !f.category().isBlank()) {
                p.add(cb.equal(cb.lower(root.get("category")), f.category().toLowerCase()));
            }
            if (f.minPrice() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("price"), BigDecimal.valueOf(f.minPrice())));
            }
            if (f.maxPrice() != null) {
                p.add(cb.lessThanOrEqualTo(root.get("price"), BigDecimal.valueOf(f.maxPrice())));
            }
            if (Boolean.TRUE.equals(f.inStock())) {
                p.add(cb.greaterThan(root.get("stock"), 0));
            }
            if (f.search() != null && !f.search().isBlank()) {
                p.add(cb.like(cb.lower(root.get("name")), "%" + escapeLike(f.search().toLowerCase()) + "%", '\\'));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
