package org.commcare.formplayer.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.commcare.formplayer.beans.InstallRequestBean;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.util.RequestUtils;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

import lombok.extern.java.Log;

/**
 * Locks a public web apps session to the app, session endpoint, and identity that HQ bound its
 * one-time link to. For a public session these values carried in the request are replaced with the
 * HMAC-authenticated values from HQ's session_details response, so a recipient cannot navigate to
 * any other app/form or key storage off an arbitrary user (all are otherwise client-supplied).
 *
 * Runs before {@link AppInstallAspect}, which keys the sandbox DB off the request's app id and
 * username, so the storage factory, the MenuSession build, and endpoint navigation all see the
 * authoritative values. Ordered ahead of every other formplayer aspect.
 */
@Aspect
@Order(0)
@Log
public class PublicSessionLockAspect {

    @Before(value = "@annotation(org.commcare.formplayer.annotations.AppInstall)")
    public void lockToPublicApp(JoinPoint joinPoint) {
        Optional<HqUserDetailsBean> userDetails = RequestUtils.getUserDetails();
        if (userDetails.isEmpty() || !userDetails.get().isPublicSession()) {
            return;
        }
        HqUserDetailsBean details = userDetails.get();
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof InstallRequestBean requestBean)) {
            return;
        }

        // Fail closed: a public session must carry HQ's authoritative app id. A missing value means
        // a misconfigured or out-of-date HQ; never fall back to the client-supplied app id.
        if (!StringUtils.hasText(details.getPublicAppId())) {
            throw new IllegalStateException(
                    "Public web apps session is missing an authoritative app id from HQ");
        }
        if (!Objects.equals(requestBean.getAppId(), details.getPublicAppId())) {
            log.warning("Public session request app id did not match the authoritative value; "
                    + "using the authoritative app id");
        }
        requestBean.setAppId(details.getPublicAppId());
        requestBean.setUsername(details.getUsername());
        requestBean.setDomain(details.getDomain());
        requestBean.setRestoreAs(null);
        requestBean.setRestoreAsCaseId(null);

        if (requestBean instanceof SessionNavigationBean navigationBean) {
            if (!StringUtils.hasText(details.getPublicEndpointId())) {
                throw new IllegalStateException(
                        "Public web apps session is missing an authoritative endpoint id from HQ");
            }
            boolean clientDiffers = !Objects.equals(navigationBean.getEndpointId(),
                    details.getPublicEndpointId())
                    || (navigationBean.getEndpointArgs() != null
                    && !navigationBean.getEndpointArgs().isEmpty());
            if (clientDiffers) {
                log.warning("Public session request endpoint/args did not match the authoritative "
                        + "endpoint; using the authoritative endpoint with no args");
            }
            navigationBean.setEndpointId(details.getPublicEndpointId());
            // Public sessions have an empty restore, so endpoint args (e.g. case ids) cannot
            // resolve; the designated public endpoint must take no required arguments.
            navigationBean.setEndpointArgs(null);
        }
    }
}
