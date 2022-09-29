package org.commcare.formplayer.junit;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Mockito answer for mocking restores.
 */
public class RestoreFactoryAnswer implements Answer {
    private String mRestoreFile;

    public RestoreFactoryAnswer(String restoreFile) {
        mRestoreFile = restoreFile;
    }

    @Override
    public InputStream answer(InvocationOnMock invocation) throws Throwable {
        return new FileInputStream("src/test/resources/" + mRestoreFile);
    }
}
