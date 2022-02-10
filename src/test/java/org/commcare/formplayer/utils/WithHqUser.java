/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.commcare.formplayer.utils;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When used with {@link WithSecurityContextTestExecutionListener} this annotation can be
 * added to a test method to emulate running with a mocked user. In order to work with
 * {@link MockMvc} The {@link SecurityContext} that is used will have the following
 * properties:
 *
 * <ul>
 * <li>The {@link SecurityContext} created with be that of
 * {@link SecurityContextHolder#createEmptyContext()}</li>
 * <li>It will be populated with an {@link org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken} that uses
 * the username of either {@link #value()} or {@link #username()},
 * domains that are specified by {@link #domains()}, the 'current domain'
 * specified by {@link #domain()},
 * superuser status specified by {@link #isSuperUser()},
 * list of enabled previews specified by {@link #enabledPreviews()} and
 * list of enabled toggles specified by {@link #enabledToggles()}.
 * </ul>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = WithHqUserSecurityContextFactory.class)
public @interface WithHqUser {

    /**
     * Convenience mechanism for specifying the username. The default is "user". If
     * {@link #username()} is specified it will be used instead of {@link #value()}
     */
    String value() default "user";

    /**
     * The username to be used. Note that {@link #value()} is a synonym for
     * {@link #username()}, but if {@link #username()} is specified it will take
     * precedence.
     */
    String username() default "";

    /**
     * The domains to use. The default is "domaim".
     */
    String[] domains() default {"domain"};

    /**
     * The domain the user details was created for. This represents the domain of the
     * request. The default value is "domain".
     */
    String domain() default "domain";

    /**
     * Set whether of not the user is a superuser. Defaults to false.
     */
    boolean isSuperUser() default false;

    /**
     * List of enabled previews for the user. Defaults to a mock list of preview_a and preview_b
     */
    String[] enabledPreviews() default {"preview_a", "preview_b"};


    /**
     * List of enabled toggles for the user. Defaults to a mock list of toggle_a and toggle_b
     */
    String[] enabledToggles() default {"toggle_a", "toggle_b"};
}
