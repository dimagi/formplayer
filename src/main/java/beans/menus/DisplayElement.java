package beans.menus;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.commcare.suite.model.Action;
import org.javarosa.core.model.condition.EvaluationContext;

/**
 * Created by willpride on 4/14/16.
 */
public class DisplayElement {
    private String text;
    private String audioUri;
    private String imageUri;

    public DisplayElement(){}

    public DisplayElement(Action action, EvaluationContext ec){
        this.text = action.getDisplay().getText().evaluate(ec);
        if(action.getDisplay().getAudioURI() != null) {
            this.audioUri = action.getDisplay().getAudioURI().evaluate(ec);
        }
        if(action.getDisplay().getImageURI() != null) {
            this.imageUri = action.getDisplay().getImageURI().evaluate(ec);
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
}
