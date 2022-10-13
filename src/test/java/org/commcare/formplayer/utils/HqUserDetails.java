package org.commcare.formplayer.utils;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * This class is used to construct {@link HqUserDetailsBean} classes for use in tests.
 * Example usage:
 *
 * <pre class="code">
 *   HqUserDetails details = HqUserDetails.builder()
 *       .username("bob")
 *       .domain("my-domain")
 *       .build();
 *   HqUserDetailsBean bean = details.toBean();
 * </pre>
 *
 * @see WithHqUserSecurityContextFactory
 */
@Builder
@AllArgsConstructor
public class HqUserDetails {
    private String username;
    private String domain;
    private String[] domains;
    private boolean isSuperUser;
    private String[] enabledPreviews;
    private String[] enabledToggles;

    public HqUserDetails(WithHqUser withUser) {
        String username = StringUtils.hasLength(withUser.username()) ? withUser.username()
                : withUser.value();
        Assert.notNull(username, () -> withUser
                + " cannot have null username on both username and value properties");
        this.username = username;
        this.domain = withUser.domain();
        this.domains = withUser.domains();
        this.isSuperUser = withUser.isSuperUser();
        this.enabledPreviews = withUser.enabledPreviews();
        this.enabledToggles = withUser.enabledToggles();
    }

    public HqUserDetailsBean toBean() {
        return new HqUserDetailsBean(domain, domains, username, isSuperUser, enabledToggles, enabledPreviews);
    }
}
