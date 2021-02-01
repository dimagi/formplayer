package org.commcare.formplayer.util;

import com.timgroup.statsd.StatsDClient;

import java.util.List;
import java.util.ArrayList;


public class FormplayerDatadog {

    class Tag {
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
    private List<Tag> tags;

    public FormplayerDatadog(StatsDClient datadogClient) {
        this.datadogClient = datadogClient;
        this.tags = new ArrayList<>();
    }

    // Tag related methods
    public List<Tag> getTags() {
        return this.tags;
    }

    public void addTag(String name, String value) {
        Tag tag = new Tag(name, value);
        tags.add(tag);
    }

    public void clearTags() {
         tags.clear();
    }

    // Datadog API Wrappers
    public void recordExecutionTime(String aspect, final long value, List<String> additionalTags) {
        List<String> tagsToSend = new ArrayList<>(additionalTags);
        for (Tag tag : tags) {
            tagsToSend.add(tag.formatted());
        }

        String[] datadogTags = tagsToSend.toArray(new String[tagsToSend.size()]);
        datadogClient.recordExecutionTime(aspect, value, datadogTags);
    }

    public void increment(String aspect, List<String> additionalTags) {
        List<String> tagsToSend = new ArrayList<>(additionalTags);
        for (Tag tag : tags) {
            tagsToSend.add(tag.formatted());
        }

        String[] datadogTags = tagsToSend.toArray(new String[tagsToSend.size()]);
        datadogClient.increment(aspect, datadogTags);
    }

}
