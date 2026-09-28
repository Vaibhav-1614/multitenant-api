package com.example.multitenantapi.tenant;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantDTO {
    private Long id;
    private String name;
    private String plan;
    private Instant createdAt;
}
