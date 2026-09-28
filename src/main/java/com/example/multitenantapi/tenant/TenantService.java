package com.example.multitenantapi.tenant;

import com.example.multitenantapi.entity.Tenant;
import com.example.multitenantapi.entity.User;
import com.example.multitenantapi.exception.ResourceNotFoundException;
import com.example.multitenantapi.repository.TenantRepository;
import com.example.multitenantapi.repository.UserRepository;
import com.example.multitenantapi.security.SecurityUtils;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public TenantDTO currentTenant() {
        Tenant tenant = loadCurrentTenant();
        return TenantDTO.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .plan(tenant.getPlan())
                .createdAt(tenant.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TenantUserDTO> listUsers() {
        return userRepository.findAllByTenantIdOrderByIdAsc(SecurityUtils.currentTenantId())
                .stream()
                .map(TenantService::toDto)
                .toList();
    }

    @Transactional
    public TenantUserDTO addUser(CreateTenantUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        User saved = userRepository.save(User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .tenant(loadCurrentTenant())
                .build());
        return toDto(saved);
    }

    private Tenant loadCurrentTenant() {
        return tenantRepository.findById(SecurityUtils.currentTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private static TenantUserDTO toDto(User user) {
        return TenantUserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
