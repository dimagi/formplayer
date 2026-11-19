package org.commcare.formplayer.auth;

import lombok.Value;

/**
 * Typed credential for a public web apps session (one-time link).
 *
 * Wraps the value of the {@code public_form_session_key} cookie so that the
 * {@link org.commcare.formplayer.services.HqUserDetailsService} can distinguish a public session
 * from a regular Django session.
 */
@Value
public class PublicSessionCredential {
    String sessionKey;
}
