package com.invoisuite.server.repository;

import com.invoisuite.server.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByCompanyIdOrderByIssueDateDescIdDesc(Long companyId);

    Optional<Invoice> findByIdAndCompanyId(Long id, Long companyId);

    long countByCompanyIdAndIssueDateBetween(Long companyId, java.time.LocalDate from, java.time.LocalDate to);
}
