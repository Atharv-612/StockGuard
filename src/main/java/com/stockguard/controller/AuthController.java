package com.stockguard.controller;

import com.stockguard.config.RateLimitingFilter;
import com.stockguard.dto.AuthDto;
import com.stockguard.service.AuditService;
import com.stockguard.service.AuthService;
import com.stockguard.service.JwtService;
import com.stockguard.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklistService tokenBlacklistService;
    private final com.stockguard.service.TokenBlocklistService tokenBlocklistService;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthController(AuthService authService,
                          TokenBlacklistService tokenBlacklistService,
                          com.stockguard.service.TokenBlocklistService tokenBlocklistService,
                          JwtService jwtService,
                          AuditService auditService) {
        this.authService = authService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.tokenBlocklistService = tokenBlocklistService;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDto.AuthResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        AuthDto.AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.AuthResponse> login(@Valid @RequestBody AuthDto.LoginRequest request,
                                                      HttpServletRequest httpRequest) {
        try {
            AuthDto.AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            String clientIp = RateLimitingFilter.getClientIp(httpRequest);
            auditService.logSecurityEvent("LOGIN_FAILED", request.getEmail(),
                    "Invalid email or password attempt from IP " + clientIp, clientIp, "FAILURE");
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            try {
                Date expiration = jwtService.extractExpiration(jwt);
                String email = jwtService.extractUsername(jwt);
                String jti = jwtService.extractJti(jwt);

                tokenBlacklistService.blacklistToken(jwt, expiration);
                if (jti != null) {
                    tokenBlocklistService.revokeToken(jti, email, expiration, "LOGOUT");
                }

                String clientIp = RateLimitingFilter.getClientIp(request);
                auditService.logSecurityEvent("LOGOUT", email, "User logged out; token invalidated", clientIp, "SUCCESS");
            } catch (Exception e) {
                // Token might already be expired or malformed
            }
        }
        return ResponseEntity.ok(Map.of("message", "User logged out and session token invalidated successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDto.AuthResponse> getCurrentUser() {
        var user = authService.getCurrentAuthenticatedUser();
        var shop = user.getShop();
        AuthDto.AuthResponse resp = new AuthDto.AuthResponse(
                null,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                shop.getId(),
                shop.getName(),
                shop.getCurrency()
        );
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'OWNER')")
    public ResponseEntity<AuthDto.UserResponse> createTeamMember(@Valid @RequestBody AuthDto.CreateUserRequest request) {
        AuthDto.UserResponse response = authService.createUserForShop(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'OWNER')")
    public ResponseEntity<List<AuthDto.UserResponse>> getTeamMembers() {
        return ResponseEntity.ok(authService.getUsersForCurrentShop());
    }
}
