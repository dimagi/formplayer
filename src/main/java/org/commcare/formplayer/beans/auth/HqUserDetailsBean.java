package org.commcare.formplayer.beans.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class HqUserDetailsBean implements UserDetails {
    private String[] domains;
    private int djangoUserId;
    private boolean isSuperUser;
    private String username;
    private String authToken;
    private String domain;

    public HqUserDetailsBean() {
    }

    public HqUserDetailsBean(String domain, String[] domains, String username, boolean isSuperuser) {
        this.domain = domain;
        this.domains = domains;
        this.username = username;
        this.isSuperUser = isSuperuser;
    }

    public boolean isAuthorized(String domain, String username) {
        return isSuperUser || Arrays.asList(domains).contains(domain) && this.username.equals(username);
    }

    /////////////////////// UserDetails methods

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
