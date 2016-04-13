package beans.menus;

import io.swagger.annotations.Api;

/**
 * Created by willpride on 4/13/16.
 */
@Api(description = "A menu command")
public class Command {
    private int index;
    private String displayText;
    private String audioUri;
    private String imageUri;

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
}
