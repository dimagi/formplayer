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
        Mockito.reset(webClientMock);
        when(webClientMock.caseClaimPost(anyString(), any())).thenReturn(returnValue);

        return () -> {
            VerificationMode once = Mockito.times(1);
            verify(webClientMock, once).caseClaimPost(anyString(), any());
        };
    }

    /**
     * Mock the restore and verify it happened
     */
    public VerifiedMock mockRestore(String restoreFile) {
        Mockito.reset(restoreFactoryMock);
        RestoreFactoryAnswer answer = new RestoreFactoryAnswer(restoreFile);
        Mockito.doAnswer(answer).when(restoreFactoryMock).getRestoreXml(anyBoolean());

        return () -> {
            VerificationMode once = Mockito.times(1);
            verify(restoreFactoryMock, once).getRestoreXml(anyBoolean());
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
