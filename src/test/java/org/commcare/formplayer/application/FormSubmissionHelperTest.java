package org.commcare.formplayer.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.commcare.formplayer.services.FormSessionService;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.util.RequestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Unit tests for {@link FormSubmissionHelper#onSuccessfulSubmit}: a public web apps session's
 * sandbox is marked for deletion once its single form is submitted; a regular session is untouched.
 */
public class FormSubmissionHelperTest {

    private FormSubmissionHelper helperWith(FormSessionService formSessionService,
            RestoreFactory restoreFactory) {
        FormSubmissionHelper helper = new FormSubmissionHelper();
        helper.formSessionService = formSessionService;
        helper.restoreFactory = restoreFactory;
        return helper;
    }

    @Test
    public void onSuccessfulSubmit_publicSession_marksSandboxForDeletion() {
        FormSessionService formSessionService = mock(FormSessionService.class);
        RestoreFactory restoreFactory = mock(RestoreFactory.class);
        FormSubmissionHelper helper = helperWith(formSessionService, restoreFactory);

        try (MockedStatic<RequestUtils> mocked = mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::isPublicSession).thenReturn(true);
            helper.onSuccessfulSubmit("session-1");
        }

        verify(formSessionService).deleteSessionById("session-1");
        verify(restoreFactory).markPublicSandboxForDeletion();
    }

    @Test
    public void onSuccessfulSubmit_regularSession_leavesSandboxInPlace() {
        FormSessionService formSessionService = mock(FormSessionService.class);
        RestoreFactory restoreFactory = mock(RestoreFactory.class);
        FormSubmissionHelper helper = helperWith(formSessionService, restoreFactory);

        try (MockedStatic<RequestUtils> mocked = mockStatic(RequestUtils.class)) {
            mocked.when(RequestUtils::isPublicSession).thenReturn(false);
            helper.onSuccessfulSubmit("session-1");
        }

        verify(formSessionService).deleteSessionById("session-1");
        verify(restoreFactory, never()).markPublicSandboxForDeletion();
    }
}
