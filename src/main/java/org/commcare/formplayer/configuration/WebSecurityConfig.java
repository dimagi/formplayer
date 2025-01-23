package org.commcare.formplayer.configuration;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasAuthority;

import org.commcare.formplayer.auth.CommCareSessionAuthFilter;
import org.commcare.formplayer.auth.HmacAuthFilter;
import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.services.HqUserDetailsService;
import org.commcare.formplayer.util.Constants;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;

@Configuration
public class WebSecurityConfig {
    @Autowired
    private HqUserDetailsService userDetailsService;

    @Autowired
    private FormSessionService formSessionService;

    @Value("${commcarehq.formplayerAuthKey}")
    private String formplayerAuthKey;

    @Value("${formplayer.allowDoubleSlash:true}")
    private boolean allowDoubleSlash;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        disableDefaults(http);

        http.authorizeHttpRequests(auth -> auth
                // no auth required for management endpoints
                // (these are bound to a separate HTTP port that is not publicly exposed)
                .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()

                // not auth required
                .requestMatchers(new AntPathRequestMatcher("/serverup")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/favicon.ico")).permitAll()

                // validate form auth
                .requestMatchers(new AntPathRequestMatcher("/validate_form")).access(
                        getFormValidationAuthManager())

                // full auth required for all other requests
                .anyRequest().authenticated()
        );

        // Configure the authentication manager with a provider that will use the
        // ``HQUserDetailsService`` to authenticate the user
        ProviderManager authenticationManager = getAuthManager();
        http.authenticationManager(authenticationManager);

        // configure auth filters
        http.addFilterAt(getHmacAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(getSessionAuthFilter(authenticationManager), HmacAuthFilter.class);

        // By setting the csrfRequestAttributeName to null, the CsrfToken must first be loaded to determine what
        // attribute name to use. This causes the CsrfToken to be loaded on every request.
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http.csrf(it -> it
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(new RequestHeaderRequestMatcher(Constants.HMAC_HEADER))
                .csrfTokenRequestHandler(requestHandler)
        );
        http.cors(Customizer.withDefaults());
        return http.build();
    }

    @NotNull
    private ProviderManager getAuthManager() {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(userDetailsService);
        return new ProviderManager(provider);
    }

    /**
     * @return AuthroizationManager to authenticate 'validate_form' requests. These requests
     *         have relaxed auth requirements of either an authenticated user or
     *         any user with the 'COMMCARE' authority (this gets set by HMAC auth).
     */
    private AuthorizationManager<RequestAuthorizationContext> getFormValidationAuthManager() {
        // "isAuthenticated() or hasAuthority('COMMCARE')"
        return (authentication, request) -> {
            AuthenticatedAuthorizationManager<Object> isAuthed = AuthenticatedAuthorizationManager.authenticated();
            AuthorizationDecision authDecision = isAuthed.check(authentication, request);
            if (authDecision.isGranted()) {
                return authDecision;
            }
            return hasAuthority(Constants.AUTHORITY_COMMCARE).check(authentication, request);

        };
    }

    private HmacAuthFilter getHmacAuthFilter() {
        return HmacAuthFilter.builder()
                .hmacKey(formplayerAuthKey)
                .formSessionService(formSessionService)
                .build();
    }

    private void disableDefaults(HttpSecurity http) throws Exception {
        http.sessionManagement((sessions) -> sessions
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );
        http.requestCache(RequestCacheConfigurer::disable); // only needed for login workflow
        http.logout(AbstractHttpConfigurer::disable);
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
    }

    public CommCareSessionAuthFilter getSessionAuthFilter(AuthenticationManager authenticationManager) throws Exception {
        CommCareSessionAuthFilter filter = new CommCareSessionAuthFilter();
        filter.setAuthenticationManager(authenticationManager);
        filter.setRequiresAuthenticationRequestMatcher(new CommCareSessionAuthFilter.SessionAuthRequestMatcher());
        return filter;
    }
}
