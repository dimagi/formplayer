package beans.menus;

import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;

/**
 * Created by willpride on 4/13/16.
 */
public class Style {

    private DisplayFormat displayFormats;
    private int fontSize;
    private String widthHint;

    public Style(){}

    public Style(DetailField detail){
        if(detail.getFontSize() != null) {
            try {
                setFontSize(Integer.parseInt(detail.getFontSize()));
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }
        setDisplayFormatFromString(detail.getTemplateForm());
        setWidthHint(detail.getTemplateWidthHint());
    }

    enum DisplayFormat {
        Image,
        Audio,
        Text,
        Graph
    }

    public DisplayFormat getDisplayFormat() {
        return displayFormats;
    }

    private void setDisplayFormat(DisplayFormat displayFormats) {
        this.displayFormats = displayFormats;
    }

    public int getFontSize() {
        return fontSize;
    }

    private void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getWidthHint() {
        return widthHint;
    }

    private void setWidthHint(String widthHint) {
        this.widthHint = widthHint;
    }

    private void setDisplayFormatFromString(String displayFormat){
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
