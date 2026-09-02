package com.slmanju.ceylonads.category.repository;

import com.slmanju.ceylonads.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlugAndActiveTrue(String slug);
    Optional<Category> findBySlug(String slug);
    List<Category> findAllByActiveTrueOrderByDisplayOrderAscNameAsc();

    // Tuition filter metadata: the vertical's direct children, without walking the whole tree.
    List<Category> findByParentIdAndActiveTrueOrderByDisplayOrderAscIdAsc(Long parentId);
}
