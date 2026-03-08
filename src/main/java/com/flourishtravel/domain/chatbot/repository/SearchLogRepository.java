package com.flourishtravel.domain.chatbot.repository;

import com.flourishtravel.domain.chatbot.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, UUID> {
}
