package com.example.multitenantapi.tenant;

import com.example.multitenantapi.entity.UserRole;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantUserDTO {
    private Long id;
    private String email;
    private UserRole role;
    private Instant createdAt;
}
