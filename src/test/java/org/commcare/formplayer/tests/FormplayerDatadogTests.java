package org.commcare.formplayer.tests;

import com.timgroup.statsd.StatsDClient;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.util.FormplayerDatadog;
import org.commcare.formplayer.utils.TestContext;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@WebMvcTest
@ContextConfiguration(classes = TestContext.class)
public class FormplayerDatadogTests {
    FormplayerDatadog datadog;
    StatsDClient mockDatadogClient;

    @BeforeEach
    public void setUp() throws Exception {
        mockDatadogClient = mock(StatsDClient.class);
        List<String> eligibleDomainsForDetailedTagging = Arrays.asList("eligible_domain");
        List<String> detailedTagNames = Arrays.asList("detailed_tag");
        datadog = new FormplayerDatadog(mockDatadogClient, eligibleDomainsForDetailedTagging, detailedTagNames);
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
        datadog.increment(Constants.DATADOG_REQUESTS, Collections.emptyList());
        String expectedTag = "form:test-form";
        String[] args = {expectedTag};
        verify(mockDatadogClient).increment(Constants.DATADOG_REQUESTS, args);
    }

    @Test
    public void testTagUsedForRecordExecutionTime() {
        datadog.addRequestScopedTag("form", "test-form");
        datadog.recordExecutionTime(Constants.DATADOG_REQUESTS, 100, Collections.emptyList());
        String expectedTag = "form:test-form";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime(Constants.DATADOG_REQUESTS, 100, args);
    }

    @Test
    public void testTransientTagUsedForIncrement() {
        datadog.addRequestScopedTag("form", "test-form");
        List<FormplayerDatadog.Tag> transientTags = new ArrayList<>();
        transientTags.add(new FormplayerDatadog.Tag("form", "test-form-2"));
        datadog.increment(Constants.DATADOG_REQUESTS, transientTags);
        String expectedTag = "form:test-form-2";
        String[] args = {expectedTag};
        verify(mockDatadogClient).increment(Constants.DATADOG_REQUESTS, args);
    }

    @Test
    public void testTransientTagUsedForRecordExecutionTime() {
        datadog.addRequestScopedTag("form", "test-form");
        List<FormplayerDatadog.Tag> transientTags = new ArrayList<>();
        transientTags.add(new FormplayerDatadog.Tag("form", "test-form-2"));
        datadog.recordExecutionTime(Constants.DATADOG_REQUESTS, 100, transientTags);
        String expectedTag = "form:test-form-2";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime(Constants.DATADOG_REQUESTS, 100, args);
    }

    @Test
    public void testAddRequestScopedDetailedTagForEligibleDomain() {
        String domain = "eligible_domain";
        datadog.setDomain(domain);
        datadog.addRequestScopedTag("detailed_tag", "test_value");
        datadog.recordExecutionTime(Constants.DATADOG_REQUESTS, 100, Collections.emptyList());
        String expectedTag = "detailed_tag:test_value";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime(Constants.DATADOG_REQUESTS, 100, args);
    }

    @Test
    public void testAddRequestScopedDetailedTagForIneligibleDomain() {
        String domain = "ineligible_domain";
        datadog.setDomain(domain);
        datadog.addRequestScopedTag("detailed_tag", "test_value");
        datadog.recordExecutionTime(Constants.DATADOG_REQUESTS, 100, Collections.emptyList());
        String expectedTag = "detailed_tag:_other";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime(Constants.DATADOG_REQUESTS, 100, args);
    }

    @Test
    public void testAddRequestScopedDetailedTagForNullDomain() {
        datadog.addRequestScopedTag("detailed_tag", "test_value");
        datadog.recordExecutionTime(Constants.DATADOG_REQUESTS, 100, Collections.emptyList());
        String expectedTag = "detailed_tag:_other";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime(Constants.DATADOG_REQUESTS, 100, args);
    }

    @Test
    public void testAddTransientDetailedTagForEligibleDomain() {
        String domain = "eligible_domain";
        datadog.setDomain(domain);
        List<FormplayerDatadog.Tag> transientTags = new ArrayList<>();
        transientTags.add(new FormplayerDatadog.Tag("detailed_tag", "test_value"));
        datadog.recordExecutionTime(Constants.DATADOG_REQUESTS, 100, transientTags);
        String expectedTag = "detailed_tag:test_value";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime(Constants.DATADOG_REQUESTS, 100, args);
    }

    @Test
    public void testAddTransientDetailedTagForIneligibleDomain() {
        // detailed tags should not be added if domain is ineligible
        String domain = "ineligible_domain";
        datadog.setDomain(domain);
        List<FormplayerDatadog.Tag> transientTags = new ArrayList<>();
        transientTags.add(new FormplayerDatadog.Tag("detailed_tag", "test_value"));
        datadog.recordExecutionTime(Constants.DATADOG_REQUESTS, 100, transientTags);
        String expectedTag = "detailed_tag:_other";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime(Constants.DATADOG_REQUESTS, 100, args);
    }

    @Test
    public void testAddTransientDetailedTagForNullDomain() {
        List<FormplayerDatadog.Tag> transientTags = new ArrayList<>();
        transientTags.add(new FormplayerDatadog.Tag("detailed_tag", "test_value"));
        datadog.recordExecutionTime(Constants.DATADOG_REQUESTS, 100, transientTags);
        String expectedTag = "detailed_tag:_other";
        String[] args = {expectedTag};
        verify(mockDatadogClient).recordExecutionTime(Constants.DATADOG_REQUESTS, 100, args);
    }

}
