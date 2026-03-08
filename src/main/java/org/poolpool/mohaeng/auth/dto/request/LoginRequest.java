// LoginRequest.java
package org.poolpool.mohaeng.auth.dto.request;

public record LoginRequest(String userId, String userPwd, String name, String phone) {}