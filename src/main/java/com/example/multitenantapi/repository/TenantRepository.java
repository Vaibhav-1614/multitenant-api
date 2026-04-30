package com.example.multitenantapi.repository;

import com.example.multitenantapi.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
}
