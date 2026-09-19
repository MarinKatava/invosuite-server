package com.invoisuite.server.repository;

import com.invoisuite.server.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByCompanyIdOrderByNameAsc(Long companyId);

    Optional<Client> findByIdAndCompanyId(Long id, Long companyId);
}
