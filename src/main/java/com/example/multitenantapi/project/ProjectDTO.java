package com.example.multitenantapi.project;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectDTO {
    private Long id;

    @NotBlank
    private String name;

    private String description;
}
