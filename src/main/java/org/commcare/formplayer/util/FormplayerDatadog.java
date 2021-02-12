package org.commcare.formplayer.util;

import com.timgroup.statsd.StatsDClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper for the Datadog Java client
 * Provides the ability to set tags throughout the request that will be appended to every datadog request
 * for the remainder of the object's life (scoped at a formplayer request level)
 */
public class FormplayerDatadog {

    /**
     * Internal class to represent tag and easily format for sending to datadog
     */
    public static class Tag {
        private String name;
        private String value;

        public Tag(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String formatted() {
            return name + ":" + value;
        }
    }

    /** Datadog's java client that communicates with agent */
    private StatsDClient datadogClient;
    /** List of domains that we want detailed tags enabled for. Set in application.properties */
    private Set<String> domainsWithDetailedTagging;
    /** Tags only eligible for domains with detailed tagging enabled. Set in application.properties */
    private Set<String> detailedTagNames;
    /** Tags appended to every datadog request */
    private Map<String, Tag> requestScopedTags;
    /** Domain that formplayer request originates from */
    private String domain;

    // Constructor, Getters, Setters
    public FormplayerDatadog(StatsDClient datadogClient,
                             List<String> domainsWithDetailedTagging,
                             List<String> detailedTagNames) {
        this.datadogClient = datadogClient;
        this.domainsWithDetailedTagging = new HashSet<>(domainsWithDetailedTagging);
        this.detailedTagNames = new HashSet<>(detailedTagNames);
        this.requestScopedTags = new HashMap<>();
    }

    public List<Tag> getRequestScopedTags() {
        return new ArrayList<>(this.requestScopedTags.values());
    }

    public Set<String> getDetailedTagNames() {
        return this.detailedTagNames;
    }

    public Set<String> getDomainsWithDetailedTagging() {
        return this.domainsWithDetailedTagging;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    // Tag Management Methods

    /**
     * Adds a tag that will be used for the remainder of the request
     * If the tag is not eligible for this domain, it will not be added
     * @param name - name of the tag (e.g., domain)
     * @param value - value of the tag (e.g., test_domain)
     */
    public void addRequestScopedTag(String name, String value) {
        // ensure tag is eligible for this domain before adding
        if (shouldAddTag(name)) {
            Tag tag = new Tag(name, value);
            requestScopedTags.put(name, tag);
        }
    }

    /**
     * Removes all request level tags
     */
    public void clearRequestScopedTags() {
         requestScopedTags.clear();
    }

    // Datadog API Wrappers

    /**
     * Wrapper for StatsDClient's recordExecutionTime method
     * @param aspect - name of the metric
     * @param value - value of the metric
     * @param transientTags - "one time" tags in addition to requestScopedTags
     * NOTE: these tags have higher priority than request scoped tags (in the case of duplicate tag names)
     */
    public void recordExecutionTime(String aspect, final long value, List<Tag> transientTags) {
        List<String> tagsToSend = getTagsToSend(transientTags);
        String[] datadogTags = tagsToSend.toArray(new String[tagsToSend.size()]);
        datadogClient.recordExecutionTime(aspect, value, datadogTags);
    }

    /**
     * Wrapper for StatsDClient's increment method
     * @param aspect - name of the metric
     * @param transientTags - "one time" tags in addition to requestScopedTags
     * NOTE: these tags have higher priority than request scoped tags (in the case of duplicate tag names)
     */
    public void increment(String aspect, List<Tag> transientTags) {
        List<String> tagsToSend = getTagsToSend(transientTags);
        String[] datadogTags = tagsToSend.toArray(new String[tagsToSend.size()]);
        datadogClient.increment(aspect, datadogTags);
    }

    // Private Helpers

    /**
     * Filters out tags that are not eligible for the current domain and formats
     * @param transientTags - "one time" tags in addition to requestScopedTags
     * NOTE: these tags have higher priority than request scoped tags (in the case of duplicate tag names)
     * @return - list of formatted tags to send
     */
    private List<String> getTagsToSend(List<Tag> transientTags) {
        List<String> formattedTags = new ArrayList<>();
        // transient keys take precedence over existing keys
        HashSet<String> transientKeys = new HashSet<String>();
        
        for (Tag tag : transientTags) {
            if (shouldAddTag(tag.name)) {
                formattedTags.add(tag.formatted());
                // keep track of transient tag names
                transientKeys.add(tag.name);
            }
        }

        // append request scoped tags
        for (Tag tag : getRequestScopedTags()) {
            if (!transientKeys.contains(tag.name)) {
                formattedTags.add(tag.formatted());
            }
        }

        return formattedTags;
    }

    /**
     * Determines if the tag name is eligible for the current domain
     * NOTE: if the domain is not set and it is a detailed tag, this will return false
     * @param tagName - tag.name
     * @return boolean representing if tag should be added or not
     */
    private boolean shouldAddTag(String tagName) {
        if (getDetailedTagNames().contains(tagName)) {
             return domain != null && getDomainsWithDetailedTagging().contains(this.domain);
        } else {
            // not a detailed metric, can add
            return true;
        }
    }

}
