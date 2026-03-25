package com.mindflow.security.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.issueToken(principal.getUsername(), principal.getRole());
        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), principal.getUsername(), principal.getRole());
    }
}
