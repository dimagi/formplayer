package beans.menus;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.Api;
import org.commcare.suite.model.MenuDisplayable;

/**
 * Created by willpride on 4/13/16.
 */
@Api(description = "A menu command")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Command {
    private int index;
    private String displayText;
    private String audioUri;
    private String imageUri;

    public Command(){}

    public Command(MenuDisplayable menuDisplayable, int index){
        super();
        this.setIndex(index);
        this.setDisplayText(menuDisplayable.getDisplayText());
        this.setImageUri(menuDisplayable.getImageURI());
        this.setAudioUri(menuDisplayable.getAudioURI());
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getAudioUri() {
        return audioUri;
    }

    public void setAudioUri(String audioUri) {
        this.audioUri = audioUri;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    @Override
    public String toString(){
        return "Command [index=" + index + ", text=" + displayText + ", " +
                "audioUri=" + audioUri + ", imageUri=" + imageUri + "]";
    }
}
