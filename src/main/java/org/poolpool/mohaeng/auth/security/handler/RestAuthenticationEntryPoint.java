package org.poolpool.mohaeng.auth.security.handler;

import java.io.IOException;

// 💡 아래 임포트들을 확인하세요.
import org.poolpool.mohaeng.auth.dto.response.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 💡 핵심 수정: getWriter() 대신 getOutputStream() 사용!
        // 추가 임포트 없이 바로 사용 가능합니다.
        om.writeValue(response.getOutputStream(),
                ErrorResponse.of("UNAUTHORIZED", "인증이 필요합니다.", request.getRequestURI())
        );
    }
}