package org.commcare.formplayer.tests;

import com.timgroup.statsd.StatsDClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.utils.TestContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestContext.class)
public class FormplayerDatadogTests {
    FormplayerDatadog datadog;
    StatsDClient mockDatadogClient;

    @Before
    public void setUp() throws Exception {
        mockDatadogClient = mock(StatsDClient.class);
        datadog = new FormplayerDatadog(mockDatadogClient);
    }

    @Test
    public void testAddTag() {
        datadog.addTag("form", "test-form");
        List<FormplayerDatadog.Tag> tags = datadog.getTags();
        assertEquals(1, tags.size());
        FormplayerDatadog.Tag tag = tags.get(0);
        String formattedTag = tag.formatted();
        assertEquals("form:test-form", formattedTag);
    }

    @Test
    public void testAddTagDuplicate() {
        datadog.addTag("form", "test-form");
        datadog.addTag("form", "test-form-2");
        List<FormplayerDatadog.Tag> tags = datadog.getTags();
        assertEquals(1, tags.size());
        FormplayerDatadog.Tag tag = tags.get(0);
        String formattedTag = tag.formatted();
        assertEquals("form:test-form-2", formattedTag);
    }

    @Test
    public void testClearTags() {
        datadog.addTag("form", "test-form");
        datadog.addTag("domain", "test-domain");
        List<FormplayerDatadog.Tag> tags = datadog.getTags();
        assertEquals(2, tags.size());
        datadog.clearTags();
        tags = datadog.getTags();
        assertEquals(0, tags.size());
    }

    @Test
    public void testTagUsedForIncrement() {
        datadog.addTag("form", "test-form");
        datadog.increment(Constants.DATADOG_REQUESTS, Collections.emptyList());
        String expectedTag = "form:test-form";
        String[] args = {expectedTag};
        verify(mockDatadogClient).increment(Constants.DATADOG_REQUESTS, args);
    }

    @Test
    public void testTagUsedForRecordExecutionTime() {
        datadog.addTag("form", "test-form");
        datadog.recordExecutionTime(Constants.DATADOG_REQUESTS, 100, Collections.emptyList());
        String expectedTag = "form:test-form";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime(Constants.DATADOG_REQUESTS, 100, args);
    }

    @Test
    public void testTransientTagUsedForIncrement() {
        datadog.addTag("form", "test-form");
        List<FormplayerDatadog.Tag> transientTags = new ArrayList<>();
        transientTags.add(new FormplayerDatadog.Tag("form", "test-form-2"));
        datadog.increment(Constants.DATADOG_REQUESTS, transientTags);
        String expectedTag = "form:test-form-2";
        String[] args = {expectedTag};
        verify(mockDatadogClient).increment(Constants.DATADOG_REQUESTS, args);
    }

    @Test
    public void testTransientTagUsedForRecordExecutionTime() {
        datadog.addTag("form", "test-form");
        List<FormplayerDatadog.Tag> transientTags = new ArrayList<>();
        transientTags.add(new FormplayerDatadog.Tag("form", "test-form-2"));
        datadog.recordExecutionTime(Constants.DATADOG_REQUESTS, 100, transientTags);
        String expectedTag = "form:test-form-2";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime(Constants.DATADOG_REQUESTS, 100, args);
    }

}
