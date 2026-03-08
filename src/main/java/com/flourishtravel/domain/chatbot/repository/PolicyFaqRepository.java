package com.flourishtravel.domain.chatbot.repository;

import com.flourishtravel.domain.chatbot.entity.PolicyFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyFaqRepository extends JpaRepository<PolicyFaq, UUID> {

    Optional<PolicyFaq> findByTopicKey(String topicKey);

    List<PolicyFaq> findAllByOrderBySortOrderAsc();
}
