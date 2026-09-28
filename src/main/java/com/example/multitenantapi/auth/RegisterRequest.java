package com.example.multitenantapi.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Self-service sign-up: creates a brand-new tenant and makes the caller its first ADMIN.
 * Joining an existing tenant is only possible through an ADMIN of that tenant
 * (see {@code POST /api/tenants/me/users}).
 */
@Data
public class RegisterRequest {
    @NotBlank
    @Size(max = 120)
    private String tenantName;

    @Size(max = 40)
    private String plan;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
    private String password;
}
