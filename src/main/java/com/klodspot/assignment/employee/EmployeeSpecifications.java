package com.klodspot.assignment.employee;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class EmployeeSpecifications {

    private EmployeeSpecifications() {}

    static Specification<Employee> ownedBy(Long ownerId) {
        return (root, query, builder) ->
            ownerId == null ? builder.isNull(root.get("ownerId")) : builder.equal(root.get("ownerId"), ownerId);
    }

    static Specification<Employee> matching(String search, Department department, EmploymentStatus status) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (search != null && !search.isBlank()) {
                // Treat SQL wildcard characters as text entered by the user.
                String escaped = search
                    .strip()
                    .toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                predicates.add(
                    builder.or(
                        builder.like(builder.lower(root.get("fullName")), pattern, '\\'),
                        builder.like(builder.lower(root.get("email")), pattern, '\\')
                    )
                );
            }
            if (department != null) {
                predicates.add(builder.equal(root.get("department"), department));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
