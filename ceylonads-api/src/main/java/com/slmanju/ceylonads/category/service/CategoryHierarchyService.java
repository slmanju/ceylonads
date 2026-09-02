package com.slmanju.ceylonads.category.service;

import com.slmanju.ceylonads.category.entity.Category;
import com.slmanju.ceylonads.category.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the category tree in memory from a single query (the category table is small), so a
 * search request needs a fixed number of round trips regardless of tree depth or how many
 * descendants/ancestors a category has.
 */
@Service
public class CategoryHierarchyService {

    private final CategoryRepository categories;

    public CategoryHierarchyService(CategoryRepository categories) {
        this.categories = categories;
    }

    // category=vehicles must match ads directly under Vehicles AND every descendant category
    // (Cars, Motorcycles, ...), at any depth.
    @Transactional(readOnly = true)
    public Set<Long> descendantIdsInclusive(Category root) {
        Map<Long, List<Category>> childrenByParentId = childrenByParentId();
        Set<Long> ids = new LinkedHashSet<>();
        Deque<Category> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Category current = stack.pop();
            if (ids.add(current.getId())) {
                for (Category child : childrenByParentId.getOrDefault(current.getId(), List.of())) {
                    stack.push(child);
                }
            }
        }
        return ids;
    }

    // Root-first chain (e.g. Vehicles, Cars) so attribute definitions placed on an ancestor
    // category are treated as applying to its descendants too.
    @Transactional(readOnly = true)
    public List<Category> ancestorChainInclusive(Category leaf) {
        Map<Long, Category> byId = allActive().stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));
        LinkedList<Category> chain = new LinkedList<>();
        Category current = byId.getOrDefault(leaf.getId(), leaf);
        while (current != null) {
            chain.addFirst(current);
            Category parent = current.getParent();
            current = parent == null ? null : byId.get(parent.getId());
        }
        return chain;
    }

    private Map<Long, List<Category>> childrenByParentId() {
        return allActive().stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));
    }

    private List<Category> allActive() {
        return categories.findAllByActiveTrueOrderByDisplayOrderAscNameAsc();
    }
}
