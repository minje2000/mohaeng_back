package org.poolpool.mohaeng.auth.security.filter;

import java.io.IOException;
import java.util.List;

import org.poolpool.mohaeng.auth.exception.AuthException;
import org.poolpool.mohaeng.auth.exception.TokenExpiredException;
import org.poolpool.mohaeng.auth.token.jwt.JwtClaims;
import org.poolpool.mohaeng.auth.token.jwt.JwtTokenProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;

    public JwtAuthenticationFilter(JwtTokenProvider jwt) {
        this.jwt = jwt;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
//        System.out.println("JWT FILTER URI = " + uri);
//        String contextPath = request.getContextPath();
//        uri = uri.substring(contextPath.length());
//        System.out.println("CONTEXTPATH 제외 URI = " + uri);
        
        String method = request.getMethod();

        // 0) CORS preflight
        if (HttpMethod.OPTIONS.matches(method)) return true;

        // 1) React 내장배포 정적 리소스 + SPA 엔트리 (GET만)
        if (HttpMethod.GET.matches(method)) {
            if (uri.equals("/") || uri.equals("/index.html")) return true;
            if (uri.startsWith("/assets/") || uri.startsWith("/static/")) return true;
            if (uri.equals("/favicon.ico") || uri.equals("/manifest.json") || uri.equals("/robots.txt")) return true;
            if (uri.matches(".*\\.(js|css|map|png|jpg|jpeg|gif|svg|webp|ico)$")) return true;
        }

        // 2) auth endpoints는 permitAll
        if (uri.startsWith("/auth/")) return true;
        
        // 소셜 계정 연동(OAuth2 인증)
        if(uri.startsWith("/oauth2") || uri.startsWith("/login/oauth2")) return true;

        // 3) permitAll API (GET)
        if (HttpMethod.GET.matches(method) && uri.startsWith("/api/events")) {
            return true;
        }
      
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String auth = request.getHeader("Authorization");

        // 토큰이 없으면 그냥 통과 → SecurityConfig에서 authenticated()면 401 처리됨
        if (auth == null || auth.isBlank() || !auth.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = auth.substring("Bearer ".length()).trim();

        if (!jwt.validate(token)) {
            throw AuthException.unauthorized("INVALID_TOKEN", "유효하지 않은 토큰입니다.");
        }
        if (jwt.isExpired(token)) {
            throw new TokenExpiredException("TOKEN_EXPIRED", "AccessToken이 만료되었습니다.");
        }

        // AccessToken만 허용 (RefreshToken으로 API 호출 차단)
        if (!JwtClaims.ACCESS.equals(jwt.getType(token))) {
            throw AuthException.unauthorized("TOKEN_TYPE_INVALID", "AccessToken이 아닙니다.");
        }

        String userId = jwt.getUserId(token);
        String role = jwt.getRole(token); // ROLE_USER / ROLE_ADMIN

        var authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }
}
