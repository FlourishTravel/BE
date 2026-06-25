package com.flourishtravel.domain.guide.repository;

import com.flourishtravel.domain.guide.entity.GuideSessionExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GuideSessionExpenseRepository extends JpaRepository<GuideSessionExpense, UUID> {

    @Query("""
            SELECT e FROM GuideSessionExpense e
            JOIN FETCH e.session s
            LEFT JOIN FETCH s.tour
            WHERE s.id = :sessionId
            ORDER BY e.expenseDate DESC, e.createdAt DESC
            """)
    List<GuideSessionExpense> findBySession_IdOrderByExpenseDateDescCreatedAtDesc(@Param("sessionId") UUID sessionId);

    @Query("""
            SELECT e FROM GuideSessionExpense e
            JOIN FETCH e.session s
            LEFT JOIN FETCH s.tour
            WHERE e.status = :status
            ORDER BY e.createdAt DESC
            """)
    List<GuideSessionExpense> findByStatusOrderByCreatedAtDesc(@Param("status") String status);

    @Query("""
            SELECT e FROM GuideSessionExpense e
            JOIN FETCH e.session s
            LEFT JOIN FETCH s.tour
            ORDER BY e.createdAt DESC
            """)
    List<GuideSessionExpense> findAllWithSessionOrderByCreatedAtDesc();
}
