package org.commcare.formplayer.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.commcare.formplayer.util.FormplayerSentry;
import org.junit.jupiter.api.Test;

import io.sentry.Breadcrumb;
import io.sentry.SentryLevel;

public class FormplayerRavenTest {
    FormplayerSentry raven = new FormplayerSentry();

    @Test
    public void testBreadcrumbs() {
        Breadcrumb breadcrumb = raven.newBreadcrumb()
                .setData(
                        "a", "1",
                        "b", "2",
                        "c", "3"
                )
                .setCategory("Category X")
                .setLevel(SentryLevel.WARNING)
                .setType("navigation")
                .setMessage("This is a test.").value();

        assertEquals("1", breadcrumb.getData().get("a"));
        assertEquals("2", breadcrumb.getData().get("b"));
        assertEquals("3", breadcrumb.getData().get("c"));
        assertEquals("Category X", breadcrumb.getCategory());
        assertEquals(SentryLevel.WARNING, breadcrumb.getLevel());
        assertEquals("navigation", breadcrumb.getType());
        assertEquals("This is a test.", breadcrumb.getMessage());
    }

    /**
     * Breadcrumbs can not contain null values
     */
    @Test
    public void testBreadcrumbsWithNullData() {
        Breadcrumb breadcrumb = raven.newBreadcrumb()
                .setData("hello", null)
                .value();
        assertThat(breadcrumb.getData()).doesNotContainKey("hello");
    }
}
