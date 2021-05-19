package org.commcare.formplayer.beans.auth

/**
 * Utility class to check for enabled previews and toggles
 */
object FeatureFlagChecker {

    fun isPreviewEnabled(preview: String, userDetailsBean: HqUserDetailsBean): Boolean {
        return isGranted(HqUserDetailsBean.PREVIEW_PREFIX + preview, userDetailsBean)
    }

    fun isToggleEnabled(preview: String, userDetailsBean: HqUserDetailsBean): Boolean {
        return isGranted(HqUserDetailsBean.TOGGLE_PREFIX + preview, userDetailsBean)
    }

    private fun isGranted(role: String, userDetailsBean: HqUserDetailsBean): Boolean {
        for (grantedAuthority in userDetailsBean.authorities) {
            if(grantedAuthority.authority.contentEquals(role)){
                return true
            }
        }
        return false
    }
}
