// TokenResponse.java
package org.poolpool.mohaeng.auth.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long accessExpiresInMs,
        long refreshExpiresInMs
) {}
