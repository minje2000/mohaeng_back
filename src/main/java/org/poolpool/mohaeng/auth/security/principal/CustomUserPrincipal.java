package org.poolpool.mohaeng.auth.security.principal;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@SuppressWarnings("serial")
@Getter
public class CustomUserPrincipal implements UserDetails {

    private final String userId;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserPrincipal(String userId,
                               String password,
                               Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.password = password;
        this.authorities = authorities;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }

    // Spring Security의 username에 userId를 매핑
    @Override public String getUsername() { return userId; }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
