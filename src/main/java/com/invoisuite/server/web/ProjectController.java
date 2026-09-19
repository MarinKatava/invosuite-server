package com.invoisuite.server.web;

import com.invoisuite.server.dto.project.ProjectRequest;
import com.invoisuite.server.dto.project.ProjectResponse;
import com.invoisuite.server.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies/{companyId}/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public List<ProjectResponse> list(@PathVariable Long companyId) {
        return projectService.list(companyId);
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@PathVariable Long companyId, @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.create(companyId, request));
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable Long companyId, @PathVariable Long projectId,
                                   @Valid @RequestBody ProjectRequest request) {
        return projectService.update(companyId, projectId, request);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> delete(@PathVariable Long companyId, @PathVariable Long projectId) {
        projectService.delete(companyId, projectId);
        return ResponseEntity.noContent().build();
    }
}
