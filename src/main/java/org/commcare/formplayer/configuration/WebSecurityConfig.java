package org.commcare.formplayer.configuration;

import org.commcare.formplayer.auth.CommCareSessionAuthFilter;
import org.commcare.formplayer.auth.HmacAuthFilter;
import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.services.HqUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private HqUserDetailsService userDetailsService;

    @Autowired
    private FormSessionService formSessionService;

    @Value("${commcarehq.formplayerAuthKey}")
    private String formplayerAuthKey;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        disableDefaults(http);

        http
            .authorizeRequests()
            .antMatchers("/serverup", "/validate_form", "/favicon.ico")
            .permitAll();
        http.authorizeRequests().antMatchers("/**").authenticated();
        http.addFilterAt(getHmacAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(sessionAuthFilter(), HmacAuthFilter.class);
        http.cors();
    }

    private HmacAuthFilter getHmacAuthFilter() {
        return HmacAuthFilter.builder()
                .hmacKey(formplayerAuthKey)
                .formSessionService(formSessionService)
                .build();
    }

    private void disableDefaults(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.requestCache().disable();  // only needed for login workflow
        http.anonymous().disable();
        http.logout().disable();
        http.formLogin().disable();
        http.httpBasic().disable();
    }

    public CommCareSessionAuthFilter sessionAuthFilter() throws Exception {
        CommCareSessionAuthFilter filter = new CommCareSessionAuthFilter();
        filter.setAuthenticationManager(authenticationManagerBean());
        return filter;
    }

    /**
     * Configure the authentication manager with a provider that will use the
     * ``HQUserDetailsService`` to authenticate the user.
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
        provider.setPreAuthenticatedUserDetailsService(userDetailsService);
        auth.authenticationProvider(provider);
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
