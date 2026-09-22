package org.commcare.formplayer.application;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

import org.commcare.formplayer.beans.auth.HqUserDetailsBean;
import org.commcare.formplayer.exceptions.PermissionDeniedException;
import org.commcare.formplayer.util.RequestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

public class DebuggerControllerTest {

    private final DebuggerController controller = new DebuggerController();

    @Test
    public void deniedWhenEditDataPermissionMissing() {
        try (MockedStatic<RequestUtils> mocked = mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails)
                    .thenReturn(Optional.of(beanWithPermissions("access_web_apps")));
            assertThrows(PermissionDeniedException.class, controller::requireEditDataPermission);
        }
    }

    @Test
    public void deniedWhenNoAuthenticatedUser() {
        try (MockedStatic<RequestUtils> mocked = mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails).thenReturn(Optional.empty());
            assertThrows(PermissionDeniedException.class, controller::requireEditDataPermission);
        }
    }

    @Test
    public void grantedWhenEditDataPermissionPresent() {
        try (MockedStatic<RequestUtils> mocked = mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::getUserDetails)
                    .thenReturn(Optional.of(beanWithPermissions("edit_data")));
            assertDoesNotThrow(controller::requireEditDataPermission);
        }
    }

    private HqUserDetailsBean beanWithPermissions(String... permissions) {
        HqUserDetailsBean bean = new HqUserDetailsBean("domain", "aragorn");
        bean.setPermissions(permissions);
        return bean;
    }
}
