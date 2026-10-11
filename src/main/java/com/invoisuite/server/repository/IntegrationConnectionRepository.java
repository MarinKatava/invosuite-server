package com.invoisuite.server.repository;

import com.invoisuite.server.domain.IntegrationConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntegrationConnectionRepository extends JpaRepository<IntegrationConnection, Long> {
    List<IntegrationConnection> findByOwnerIdOrderByNameAsc(Long ownerId);

    List<IntegrationConnection> findByOwnerIdAndActiveTrueOrderByNameAsc(Long ownerId);

    List<IntegrationConnection> findByOwnerIdAndCompanyIdAndActiveTrueOrderByNameAsc(Long ownerId, Long companyId);

    Optional<IntegrationConnection> findByIdAndOwnerId(Long id, Long ownerId);
}
