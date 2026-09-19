package com.invoisuite.server.repository;

import com.invoisuite.server.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByCompanyIdOrderByNameAsc(Long companyId);

    Optional<Project> findByIdAndCompanyId(Long id, Long companyId);
}
