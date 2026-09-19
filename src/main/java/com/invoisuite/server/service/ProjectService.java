package com.invoisuite.server.service;

import com.invoisuite.server.common.ResourceNotFoundException;
import com.invoisuite.server.domain.Client;
import com.invoisuite.server.domain.Company;
import com.invoisuite.server.domain.Project;
import com.invoisuite.server.dto.project.ProjectRequest;
import com.invoisuite.server.dto.project.ProjectResponse;
import com.invoisuite.server.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CompanyService companyService;
    private final ClientService clientService;

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(Long companyId) {
        companyService.getOwned(companyId);
        return projectRepository.findByCompanyIdOrderByNameAsc(companyId).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Project getOwned(Long companyId, Long projectId) {
        companyService.getOwned(companyId);
        return projectRepository.findByIdAndCompanyId(projectId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " not found"));
    }

    @Transactional
    public ProjectResponse create(Long companyId, ProjectRequest request) {
        Company company = companyService.getOwned(companyId);
        Client client = request.clientId() != null ? clientService.getOwned(companyId, request.clientId()) : null;

        Project project = Project.builder()
                .company(company)
                .client(client)
                .name(request.name())
                .defaultUnit(request.defaultUnit())
                .defaultRate(request.defaultRate())
                .currency(request.currency() != null ? request.currency() : company.getDefaultCurrency())
                .active(request.active() == null || request.active())
                .build();
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public ProjectResponse update(Long companyId, Long projectId, ProjectRequest request) {
        Project project = getOwned(companyId, projectId);
        Client client = request.clientId() != null ? clientService.getOwned(companyId, request.clientId()) : null;

        project.setClient(client);
        project.setName(request.name());
        project.setDefaultUnit(request.defaultUnit());
        project.setDefaultRate(request.defaultRate());
        project.setCurrency(request.currency());
        project.setActive(request.active() == null || request.active());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long companyId, Long projectId) {
        projectRepository.delete(getOwned(companyId, projectId));
    }
}
