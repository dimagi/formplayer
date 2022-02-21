package org.commcare.formplayer.beans.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class HqUserDetailsBean implements UserDetails {

    public static final String TOGGLE_PREFIX = "toggle_";
    public static final String PREVIEW_PREFIX = "preview_";

    private String[] domains;
    private int djangoUserId;
    private boolean isSuperUser;
    private String username;
    private String authToken;
    private String domain;
    private String[] enabledToggles;
    private String[] enabledPreviews;

    public HqUserDetailsBean() {
    }

    public HqUserDetailsBean(String domain, String username) {
        this(domain, new String[]{domain}, username, false, new String[]{}, new String[]{});
    }

    public HqUserDetailsBean(String domain, String[] domains, String username, boolean isSuperuser,
            String[] enabledToggles, String[] enabledPreviews) {
        this.domain = domain;
        this.domains = domains;
        this.username = username;
        this.isSuperUser = isSuperuser;
        this.enabledToggles = enabledToggles;
        this.enabledPreviews = enabledPreviews;
    }

    public boolean isAuthorized(String domain, String username) {
        return isSuperUser || Arrays.asList(domains).contains(domain) && this.username.equals(
                username);
    }

    /////////////////////// UserDetails methods

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (enabledToggles != null) {
            for (String enabledToggle : enabledToggles) {
                authorities.add(new SimpleGrantedAuthority(TOGGLE_PREFIX + enabledToggle));
            }
        }
        if (enabledPreviews != null) {
            for (String enabledPreview : enabledPreviews) {
                authorities.add(new SimpleGrantedAuthority(PREVIEW_PREFIX + enabledPreview));
            }
        }
        return authorities;
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

    @JsonSetter(value = "enabled_toggles")
    public void setEnabledToggles(String[] enabledToggles) {
        this.enabledToggles = enabledToggles;
    }

    @JsonSetter(value = "enabled_previews")
    public void setEnabledPreviews(String[] enabledPreviews) {
        this.enabledPreviews = enabledPreviews;
    }
}
