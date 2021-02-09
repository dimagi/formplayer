package org.commcare.formplayer.util;

import com.timgroup.statsd.StatsDClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;


public class FormplayerDatadog {

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

    private StatsDClient datadogClient;
    private Map<String, Tag> tags;

    public FormplayerDatadog(StatsDClient datadogClient) {
        this.datadogClient = datadogClient;
        this.tags = new HashMap<>();
    }

    // Tag Management Methods
    public List<Tag> getTags() {
        return new ArrayList<>(this.tags.values());
    }

    public void addTag(String name, String value) {
        Tag tag = new Tag(name, value);
        tags.put(name, tag);
    }

    public void clearTags() {
         tags.clear();
    }

    // Datadog API Wrappers
    public void recordExecutionTime(String aspect, final long value, List<Tag> additionalTags) {
        List<String> tagsToSend = formattedTags(additionalTags);
        String[] datadogTags = tagsToSend.toArray(new String[tagsToSend.size()]);
        datadogClient.recordExecutionTime(aspect, value, datadogTags);
    }

    public void increment(String aspect, List<Tag> additionalTags) {
        List<String> tagsToSend = formattedTags(additionalTags);
        String[] datadogTags = tagsToSend.toArray(new String[tagsToSend.size()]);
        datadogClient.increment(aspect, datadogTags);
    }

    // Private Helpers
    private List<String> formattedTags(List<Tag> transientTags) {
        List<String> formattedTags = new ArrayList<>();
        // transient keys take precedence over existing keys
        HashSet<String> transientKeys = new HashSet<String>();
        
        for (Tag tag : transientTags) {
            formattedTags.add(tag.formatted());
            // keep track of transient tag names
            transientKeys.add(tag.name);
        }

        // append request scoped tags
        for (Tag tag : getTags()) {
            if (!transientKeys.contains(tag.name)) {
                formattedTags.add(tag.formatted());
            }
        }

        return formattedTags;
    }

}
