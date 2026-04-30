package com.example.multitenantapi.project;

import com.example.multitenantapi.entity.Project;
import com.example.multitenantapi.entity.Tenant;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public ProjectDTO toDto(Project project) {
        return ProjectDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .build();
    }

    public Project toEntity(ProjectDTO dto, Tenant tenant) {
        return Project.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .tenant(tenant)
                .build();
    }

    public void updateEntity(Project project, ProjectDTO dto) {
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
    }
}
