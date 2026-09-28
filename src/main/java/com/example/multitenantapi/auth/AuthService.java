package com.example.multitenantapi.auth;

import com.example.multitenantapi.entity.Tenant;
import com.example.multitenantapi.entity.User;
import com.example.multitenantapi.entity.UserRole;
import com.example.multitenantapi.repository.TenantRepository;
import com.example.multitenantapi.repository.UserRepository;
import com.example.multitenantapi.security.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final String DEFAULT_PLAN = "FREE";

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(request.getTenantName().trim())
                .plan(StringUtils.hasText(request.getPlan()) ? request.getPlan().trim().toUpperCase() : DEFAULT_PLAN)
                .build());

        User user = userRepository.save(User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ADMIN)
                .tenant(tenant)
                .build());

        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        return AuthResponse.builder()
                .accessToken(jwtUtil.generateToken(user))
                .expiresInMs(jwtUtil.getExpirationMs())
                .build();
    }
}
