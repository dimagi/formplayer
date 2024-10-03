package org.commcare.formplayer.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Multimap;

import org.commcare.formplayer.junit.RestoreFactoryAnswer;
import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.web.client.WebClient;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;

/**
 * Util class for creating mock request closeables
 */
public class MockRequestUtils {

    private WebClient webClientMock;
    private RestoreFactory restoreFactoryMock;

    public MockRequestUtils(WebClient webClient, RestoreFactory restoreFactory){
        webClientMock = webClient;
        restoreFactoryMock = restoreFactory;
    }

    /**
     * Mock the post request and verify it happened
     */
    public VerifiedMock mockPost(boolean returnValue) {
        return mockPost(returnValue, 1);
    }

    public VerifiedMock mockPost(boolean returnValue, int times) {
        Mockito.reset(webClientMock);
        when(webClientMock.caseClaimPost(anyString(), any())).thenReturn(returnValue);

        return () -> {
            verify(webClientMock, Mockito.times(times)).caseClaimPost(anyString(), any());
        };
    }

    /**
     * Mock the post request, verifies it happened, and updates the mocked restore.
     */
    public VerifiedMock mockPostandUpdateRestore(String restoreFile) {
        Mockito.reset(webClientMock);
        Answer<Boolean> answer = invocation -> {
            mockRestore(restoreFile);
            return true;
        };
        Mockito.doAnswer(answer).when(webClientMock).caseClaimPost(anyString(), any());

        return () -> {
            verify(webClientMock, Mockito.times(1)).caseClaimPost(anyString(), any());
        };
    }

    /**
     * Mock the restore and verify it happened
     */

    public VerifiedMock mockRestore(String restoreFile) {
        return mockRestore(restoreFile, 1);
    }

    public VerifiedMock mockRestore(String restoreFile, int times) {
        Mockito.reset(restoreFactoryMock);
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer(restoreFile);
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());

        return () -> {
            verify(restoreFactoryMock, Mockito.times(times)).getRestoreXml(anyBoolean());
        };
    }

    /**
     * Mock the query and verify it happened
     */
    public VerifiedMock mockQuery(String queryFile) {
        return mockQuery(queryFile, 1);
    }

    public VerifiedMock mockQuery(String queryFile, int times) {
        Mockito.reset(webClientMock);
        when(webClientMock.postFormData(anyString(), any(Multimap.class)))
                .thenReturn(FileUtils.getFile(this.getClass(), queryFile));

        return () -> verify(
                webClientMock, Mockito.times(times)).postFormData(
                anyString(), any(Multimap.class));
    }

    /**
     * Tagging class to make it clearer what is being returned by mock methods.
     */
    public static interface VerifiedMock extends AutoCloseable { }
}
