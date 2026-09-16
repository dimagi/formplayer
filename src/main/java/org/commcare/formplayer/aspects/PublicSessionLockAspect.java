package org.commcare.formplayer.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.commcare.formplayer.beans.AuthenticatedRequestBean;
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
 * Locks a public web apps session to the identity, app, and session endpoint that HQ bound its
 * one-time link to, replacing the client-supplied values in the request with the
 * HMAC-authenticated ones from session_details. Identity is pinned on every public request; the
 * app and endpoint are pinned wherever an app is installed.
 *
 * Ordered ahead of every other formplayer aspect so that {@link LockAspect} derives its lock key,
 * and {@link AppInstallAspect} keys the sandbox DB, from the authoritative values.
 */
@Aspect
@Order(0)
@Log
public class PublicSessionLockAspect {

    @Before(value = "@annotation(org.commcare.formplayer.annotations.UserRestore)")
    public void pinPublicSessionIdentity(JoinPoint joinPoint) {
        Optional<HqUserDetailsBean> userDetails = publicSessionDetails();
        if (userDetails.isEmpty()) {
            return;
        }
        HqUserDetailsBean details = userDetails.get();
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof AuthenticatedRequestBean requestBean)) {
            throw new IllegalStateException(
                    "Public web apps session reached a handler whose request identity cannot be "
                            + "pinned to the authenticated user");
        }
        requestBean.setUsername(details.getUsername());
        requestBean.setDomain(details.getDomain());
        requestBean.setRestoreAs(null);
        requestBean.setRestoreAsCaseId(null);
    }

    @Before(value = "@annotation(org.commcare.formplayer.annotations.AppInstall)")
    public void lockToPublicApp(JoinPoint joinPoint) {
        Optional<HqUserDetailsBean> userDetails = publicSessionDetails();
        if (userDetails.isEmpty()) {
            return;
        }
        HqUserDetailsBean details = userDetails.get();
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof InstallRequestBean requestBean)) {
            // Fail closed: this only runs for a public session (non-public returned above), so an
            // @AppInstall handler whose request we cannot lock must be rejected.
            throw new IllegalStateException(
                    "Public web apps session reached an @AppInstall handler whose request cannot be "
                            + "locked to the authoritative app/endpoint");
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
        requestBean.setPreview(false);

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

    private Optional<HqUserDetailsBean> publicSessionDetails() {
        return RequestUtils.getUserDetails().filter(HqUserDetailsBean::isPublicSession);
    }
}
