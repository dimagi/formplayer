package org.commcare.formplayer.tests;

import com.timgroup.statsd.StatsDClient;

import org.commcare.formplayer.utils.WithHqUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.utils.TestContext;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import static org.commcare.formplayer.util.Constants.TOGGLE_DETAILED_TAGGING;
import static org.mockito.Mockito.*;

import java.util.*;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class FormplayerDatadogTests {
    FormplayerDatadog datadog;
    StatsDClient mockDatadogClient;

    @BeforeEach
    public void setUp() throws Exception {
        mockDatadogClient = mock(StatsDClient.class);
        List<String> detailedTagNames = Collections.singletonList("detailed_tag");
        datadog = new FormplayerDatadog(mockDatadogClient, detailedTagNames);
    }

    @Test
    public void testAddRequestScopedTag() {
        datadog.addRequestScopedTag("form", "test-form");
        List<FormplayerDatadog.Tag> tags = datadog.getRequestScopedTags();
        Assertions.assertEquals(1, tags.size());
        FormplayerDatadog.Tag tag = tags.get(0);
        String formattedTag = tag.formatted();
        Assertions.assertEquals("form:test-form", formattedTag);
    }

    @Test
    public void testAddRequestScopedTagDuplicate() {
        datadog.addRequestScopedTag("form", "test-form");
        datadog.addRequestScopedTag("form", "test-form-2");
        List<FormplayerDatadog.Tag> tags = datadog.getRequestScopedTags();
        Assertions.assertEquals(1, tags.size());
        FormplayerDatadog.Tag tag = tags.get(0);
        String formattedTag = tag.formatted();
        Assertions.assertEquals("form:test-form-2", formattedTag);
    }

    @Test
    public void testClearRequestScopedTags() {
        datadog.addRequestScopedTag("form", "test-form");
        datadog.addRequestScopedTag("domain", "test-domain");
        List<FormplayerDatadog.Tag> tags = datadog.getRequestScopedTags();
        Assertions.assertEquals(2, tags.size());
        datadog.clearRequestScopedTags();
        tags = datadog.getRequestScopedTags();
        Assertions.assertEquals(0, tags.size());
    }

    @Test
    public void testTagUsedForIncrement() {
        datadog.addRequestScopedTag("form", "test-form");
        datadog.increment("requests", Collections.emptyList());
        String expectedTag = "form:test-form";
        String[] args = {expectedTag};
        verify(mockDatadogClient).increment("requests", args);
    }

    @Test
    public void testTagUsedForRecordExecutionTime() {
        datadog.addRequestScopedTag("form", "test-form");
        datadog.recordExecutionTime("requests", 100, Collections.emptyList());
        String expectedTag = "form:test-form";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime("requests", 100, args);
    }

    @Test
    public void testTransientTagUsedForIncrement() {
        datadog.addRequestScopedTag("form", "test-form");
        List<FormplayerDatadog.Tag> transientTags = new ArrayList<>();
        transientTags.add(new FormplayerDatadog.Tag("form", "test-form-2"));
        datadog.increment("requests", transientTags);
        String expectedTag = "form:test-form-2";
        String[] args = {expectedTag};
        verify(mockDatadogClient).increment("requests", args);
    }

    @Test
    public void testTransientTagUsedForRecordExecutionTime() {
        datadog.addRequestScopedTag("form", "test-form");
        List<FormplayerDatadog.Tag> transientTags = new ArrayList<>();
        transientTags.add(new FormplayerDatadog.Tag("form", "test-form-2"));
        datadog.recordExecutionTime("requests", 100, transientTags);
        String expectedTag = "form:test-form-2";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime("requests", 100, args);
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_DETAILED_TAGGING})
    public void testAddRequestScopedDetailedTagForEligibleDomain() {
        // detailed_tag was added to FormplayerDatadog in beforeTest
        datadog.addRequestScopedTag("detailed_tag", "test_value");
        datadog.recordExecutionTime("requests", 100, Collections.emptyList());
        String expectedTag = "detailed_tag:test_value";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime("requests", 100, args);
    }

    @Test
    @WithHqUser(enabledToggles = {})
    public void testAddRequestScopedDetailedTagForIneligibleDomain() {
        // detailed_tag was added to FormplayerDatadog in beforeTest
        datadog.addRequestScopedTag("detailed_tag", "test_value");
        datadog.recordExecutionTime("requests", 100, Collections.emptyList());
        String expectedTag = "detailed_tag:_other";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime("requests", 100, args);
    }

    @Test
    @WithHqUser(enabledToggles = {TOGGLE_DETAILED_TAGGING})
    public void testAddTransientDetailedTagForEligibleDomain() {
        List<FormplayerDatadog.Tag> transientTags = new ArrayList<>();
        // detailed_tag was added to FormplayerDatadog in beforeTest
        transientTags.add(new FormplayerDatadog.Tag("detailed_tag", "test_value"));
        datadog.recordExecutionTime("requests", 100, transientTags);
        String expectedTag = "detailed_tag:test_value";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime("requests", 100, args);
    }

    @Test
    @WithHqUser(enabledToggles = {})
    public void testAddTransientDetailedTagForIneligibleDomain() {
        List<FormplayerDatadog.Tag> transientTags = new ArrayList<>();
        // detailed_tag was added to FormplayerDatadog in beforeTest
        transientTags.add(new FormplayerDatadog.Tag("detailed_tag", "test_value"));
        datadog.recordExecutionTime("requests", 100, transientTags);
        String expectedTag = "detailed_tag:_other";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime("requests", 100, args);
    }

}
