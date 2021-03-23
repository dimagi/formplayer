package org.commcare.formplayer.auth;

import lombok.Value;

/**
 * Immutable objects to store the username and domain associated with a request.
 * This is used during the session auth flow as the 'principal' of the request.
 *
 * Ultimately it is passed to the HQUserDetailsService.
 */
@Value
public class UserDomainPreAuthPrincipal {
    String username;
    String domain;
}
