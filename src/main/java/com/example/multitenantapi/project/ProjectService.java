package com.example.multitenantapi.project;

import com.example.multitenantapi.entity.Project;
import com.example.multitenantapi.entity.Tenant;
import com.example.multitenantapi.exception.ResourceNotFoundException;
import com.example.multitenantapi.repository.ProjectRepository;
import com.example.multitenantapi.repository.TenantRepository;
import com.example.multitenantapi.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TenantRepository tenantRepository;
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectRepository projectRepository, TenantRepository tenantRepository, ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.tenantRepository = tenantRepository;
        this.projectMapper = projectMapper;
    }

    @Transactional(readOnly = true)
    public Page<ProjectDTO> findAll(Pageable pageable) {
        Long tenantId = SecurityUtils.currentTenantId();
        return projectRepository.findAllByTenantId(tenantId, pageable).map(projectMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ProjectDTO findById(Long id) {
        Long tenantId = SecurityUtils.currentTenantId();
        Project project = projectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return projectMapper.toDto(project);
    }

    @Transactional
    public ProjectDTO create(ProjectDTO dto) {
        Long tenantId = SecurityUtils.currentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        Project saved = projectRepository.save(projectMapper.toEntity(dto, tenant));
        return projectMapper.toDto(saved);
    }

    @Transactional
    public ProjectDTO update(Long id, ProjectDTO dto) {
        Long tenantId = SecurityUtils.currentTenantId();
        Project project = projectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        projectMapper.updateEntity(project, dto);
        return projectMapper.toDto(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = SecurityUtils.currentTenantId();
        Project project = projectRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        projectRepository.delete(project);
    }
}
