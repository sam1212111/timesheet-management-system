package com.tms.ts.service;

import com.tms.common.exception.ResourceAlreadyExistsException;
import com.tms.common.exception.ResourceNotFoundException;
import com.tms.common.util.IdGeneratorUtil;
import com.tms.ts.dto.ProjectRequest;
import com.tms.ts.dto.ProjectResponse;
import com.tms.ts.entity.Project;
import com.tms.ts.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {
    private static final String PROJECT_NOT_FOUND = "Project not found";

    private final ProjectRepository projectRepository;
    private final IdGeneratorUtil idGeneratorUtil;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                               IdGeneratorUtil idGeneratorUtil) {
        this.projectRepository = projectRepository;
        this.idGeneratorUtil = idGeneratorUtil;
    }

    @Override
    public ProjectResponse createProject(ProjectRequest request) {

        if (projectRepository.existsByCode(request.getCode())) {
            throw new ResourceAlreadyExistsException("Project code already exists");
        }

        Project project = new Project();
        project.setId(idGeneratorUtil.generateId("PRJ"));
        project.setCode(request.getCode());
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setActive(true);

        Project savedProject = projectRepository.save(project);

        return mapToProjectResponse(savedProject);
    }

    @Override
    public ProjectResponse updateProject(String id, ProjectRequest request) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROJECT_NOT_FOUND));

        if (!project.getCode().equals(request.getCode())
                && projectRepository.existsByCode(request.getCode())) {
            throw new ResourceAlreadyExistsException("Project code already exists");
        }

        project.setCode(request.getCode());
        project.setName(request.getName());
        project.setDescription(request.getDescription());

        Project savedProject = projectRepository.save(project);

        return mapToProjectResponse(savedProject);
    }

    @Override
    public ProjectResponse getProject(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROJECT_NOT_FOUND));
        return mapToProjectResponse(project);
    }

    @Override
    public List<ProjectResponse> getAllActiveProjects() {
        return projectRepository.findByActiveTrue()
                .stream()
                .map(this::mapToProjectResponse)
                .toList();
    }

    @Override
    public void deactivateProject(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROJECT_NOT_FOUND));
        project.setActive(false);
        projectRepository.save(project);
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setCode(project.getCode());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setActive(project.isActive());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());
        return response;
    }
}
