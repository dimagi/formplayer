package org.commcare.formplayer.beans.auth

import org.commcare.formplayer.util.RequestUtils

/**
 * Utility class to check for enabled previews and toggles
 */
class FeatureFlagChecker {

    companion object {

        @JvmStatic
        fun isPreviewEnabled(preview: String): Boolean {
            return isGranted(HqUserDetailsBean.PREVIEW_PREFIX + preview)
        }

        @JvmStatic
        fun isToggleEnabled(toggle: String): Boolean {
            return isGranted(HqUserDetailsBean.TOGGLE_PREFIX + toggle)
        }

        private fun isGranted(role: String): Boolean {
            val userDetailsOptional = RequestUtils.getUserDetails()
            if (userDetailsOptional.isPresent) {
                return isGranted(role, userDetailsOptional.get())
            }
            return false
        }

        private fun isGranted(role: String, userDetails: HqUserDetailsBean): Boolean {
            return userDetails.authorities.firstOrNull { grantedAuthority -> grantedAuthority.authority.contentEquals(role) } != null
        }
    }
}


