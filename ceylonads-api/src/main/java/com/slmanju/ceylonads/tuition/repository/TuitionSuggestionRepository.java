package com.slmanju.ceylonads.tuition.repository;

import com.slmanju.ceylonads.tuition.entity.SuggestionStatus;
import com.slmanju.ceylonads.tuition.entity.TuitionSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TuitionSuggestionRepository extends JpaRepository<TuitionSuggestion, Long> {

    // Admin inbox order: NEW first, then REVIEWED, then CLOSED, newest-first within each group.
    // status is stored as its enum name (EnumType.STRING), so a plain ORDER BY status would sort
    // alphabetically (CLOSED, NEW, REVIEWED) - wrong order - hence the explicit CASE ranking.
    @Query("select s from TuitionSuggestion s order by "
            + "case s.status when com.slmanju.ceylonads.tuition.entity.SuggestionStatus.NEW then 0 "
            + "when com.slmanju.ceylonads.tuition.entity.SuggestionStatus.REVIEWED then 1 "
            + "else 2 end, s.createdAt desc")
    List<TuitionSuggestion> findAllOrderByStatusPriorityThenNewest();

    long countByStatus(SuggestionStatus status);
}
