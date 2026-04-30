package com.example.multitenantapi.repository;

import com.example.multitenantapi.entity.Project;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    Page<Project> findAllByTenantId(Long tenantId, Pageable pageable);
    Optional<Project> findByIdAndTenantId(Long id, Long tenantId);
}
