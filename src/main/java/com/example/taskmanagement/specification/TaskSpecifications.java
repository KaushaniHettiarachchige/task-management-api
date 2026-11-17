package com.example.taskmanagement.specification;

import com.example.taskmanagement.entity.Task;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class TaskSpecifications {

    private TaskSpecifications() {

    }

    public static Specification<Task> ownedBy(Long userId) {
        return (root, query, cb) ->
            cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Task> containsKeyword(String keyword) {

        return (root, query, cb) -> {

            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }

            String pattern = "%"
                + keyword.trim().toLowerCase(Locale.ROOT)
                + "%";

            return cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
