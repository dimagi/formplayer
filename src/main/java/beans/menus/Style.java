package beans.menus;

/**
 * Created by willpride on 4/13/16.
 */
public class Style {

    private DisplayFormat displayFormats;
    private int fontSize;
    private String widthHint;

    enum DisplayFormat {
        Image,
        Audio,
        Text,
        Graph
    }

    public DisplayFormat getDisplayFormat() {
        return displayFormats;
    }

    public void setDisplayFormat(DisplayFormat displayFormats) {
        this.displayFormats = displayFormats;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getWidthHint() {
        return widthHint;
    }

    public void setWidthHint(String widthHint) {
        this.widthHint = widthHint;
    }

    public void setDisplayFormat(String displayFormat){
        if(displayFormat.equals("image")){
            setDisplayFormat(DisplayFormat.Image);
        } else if(displayFormat.equals("audio")){
            setDisplayFormat(DisplayFormat.Audio);
        } else if(displayFormat.equals("Text")){
            setDisplayFormat(DisplayFormat.Text);
        } else if(displayFormat.equals("Graph")){
            setDisplayFormat(DisplayFormat.Graph);
        }
    }
}
