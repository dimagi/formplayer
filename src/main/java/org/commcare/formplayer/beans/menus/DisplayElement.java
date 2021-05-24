package org.commcare.formplayer.beans.menus;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import org.commcare.suite.model.Action;
import org.commcare.suite.model.DisplayUnit;
import org.javarosa.core.model.condition.EvaluationContext;
import org.springframework.lang.Nullable;

import java.util.Arrays;


/**
 * Created by willpride on 4/14/16.
 */
public class DisplayElement {
    private String text;
    private String audioUri;
    private String imageUri;
    private String id;

    @Nullable
    private String input;

    @Nullable
    private String value;

    @Nullable
    private String receive;

    private Boolean hidden;

    @Nullable
    String[] itemsetChoices;

    @Nullable
    private String hint;

    public DisplayElement() {
    }

    public DisplayElement(Action action, EvaluationContext ec) {
        this.text = action.getDisplay().getText().evaluate(ec);
        if (action.getDisplay().getAudioURI() != null) {
            this.audioUri = action.getDisplay().getAudioURI().evaluate(ec);
        }
        if (action.getDisplay().getImageURI() != null) {
            this.imageUri = action.getDisplay().getImageURI().evaluate(ec);
        }
    }


    public DisplayElement(DisplayUnit displayUnit, EvaluationContext ec, String id, @Nullable String input,
                          @Nullable String receive, Boolean hidden, @Nullable String value, @Nullable String[] itemsetChoices) {
        this.id = id;
        this.text = displayUnit.getText().evaluate(ec);
        if (displayUnit.getImageURI() != null) {
            this.imageUri = displayUnit.getImageURI().evaluate(ec);
        }
        if (displayUnit.getAudioURI() != null) {
            this.audioUri = displayUnit.getAudioURI().evaluate(ec);
        }
        this.input = input;
        this.receive = receive;
        this.hidden = hidden;
        this.value = value;
        this.itemsetChoices = itemsetChoices;

        if (displayUnit.getHintText() != null) {
            this.hint = displayUnit.getHintText().evaluate(ec);
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @JsonGetter(value = "audio_uri")
    public String getAudioUri() {
        return audioUri;
    }

    @JsonSetter(value = "audio_uri")
    public void setAudioUri(String audioUri) {
        this.audioUri = audioUri;
    }

    @JsonGetter(value = "image_uri")
    public String getImageUri() {
        return imageUri;
    }

    @JsonSetter(value = "image_uri")
    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString(){
        return "DisplayElement id=" + id + ", text=" + text + ", value=" + value + ", imageUri=" + imageUri
                + ", audioUri=" + audioUri + ", itemsetChoices=" + Arrays.toString(itemsetChoices);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInput() {
        return input;
    }

    public String getReceive() {
        return receive;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public String[] getItemsetChoices() {
        return itemsetChoices;
    }

    @Nullable
    public String getHint() {
        return hint;
    }
}
