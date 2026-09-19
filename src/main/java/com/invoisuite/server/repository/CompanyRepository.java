package com.invoisuite.server.repository;

import com.invoisuite.server.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByOwnerIdOrderByNameAsc(Long ownerId);

    Optional<Company> findByIdAndOwnerId(Long id, Long ownerId);
}
