package org.poolpool.mohaeng.auth.security.service;

import java.util.List;

import org.poolpool.mohaeng.auth.security.principal.CustomUserPrincipal;
import org.poolpool.mohaeng.user.entity.UserEntity;
import org.poolpool.mohaeng.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        
    	// 이메일로 회원 조회
        UserEntity user = userRepository.findByEmail(email).orElseThrow(() ->
                new UsernameNotFoundException("해당 이메일의 회원이 없습니다."));

        String password = user.getUserPwd();

        // 권한 설정
        String role = "ROLE_" + user.getUserRole().name();

        return new CustomUserPrincipal(
                email,
                password,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
