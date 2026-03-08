package com.flourishtravel.domain.contact.repository;

import com.flourishtravel.domain.contact.entity.ContactRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContactRequestRepository extends JpaRepository<ContactRequest, UUID> {

    Page<ContactRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<ContactRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
