package com.example.multitenantapi.security;

import com.example.multitenantapi.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserPrincipal {
    private Long userId;
    private String email;
    private Long tenantId;
    private UserRole role;
}
