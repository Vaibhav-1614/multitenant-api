package com.example.multitenantapi.tenant;

import com.example.multitenantapi.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTenantUserRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
    private String password;

    @NotNull
    private UserRole role;
}
