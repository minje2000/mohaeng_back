package org.poolpool.mohaeng.auth.controller;

import org.poolpool.mohaeng.auth.dto.request.LoginRequest;
import org.poolpool.mohaeng.auth.dto.request.RefreshRequest;
import org.poolpool.mohaeng.auth.dto.response.TokenResponse;
import org.poolpool.mohaeng.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestHeader("RefreshToken") String refreshToken,
            @RequestBody(required = false) RefreshRequest req
    ) {
        boolean extend = (req != null && req.extendLogin());
        return ResponseEntity.ok(authService.refresh(stripBearer(refreshToken), extend));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String accessToken) {
        authService.logout(accessToken);
        return ResponseEntity.ok().body(java.util.Map.of("message", "logout ok"));
    }

    //Bearer 접두사 제거
    private String stripBearer(String header) {
        if (header == null) return null;
        return header.startsWith("Bearer ") ? header.substring("Bearer ".length()).trim() : header.trim();
    }
}
