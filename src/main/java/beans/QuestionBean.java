package beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by willpride on 1/27/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionBean {
    private String caption_audio;
    private String caption_video;
    private String caption_image;
    private String caption_markdown;
    private String caption;

    private String binding;
    private int required;
    private int relevant;
    private Object answer;
    private String datatype;

    // Style defines appearance attributes for a question
    private HashMap<String, String> style = new HashMap<>();
    private String type;
    private String ix;
    private String[] choices;
    private String repeatable;
    private String exists;
    private String addChoice;
    private String header;
    private int control;

    // The is the help/hint text defined in the formbuilder advanced section.
    // Read more about its purpose here: https://confluence.dimagi.com/display/commcarepublic/Advanced+CommCare+Android+Formatting#AdvancedCommCareAndroidFormatting-HintandHelpText
    private String help;
    private String hint;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private QuestionBean[] children;

    public String getCaption_audio() {
        return caption_audio;
    }

    public void setCaption_audio(String caption_audio) {
        this.caption_audio = caption_audio;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getBinding() {
        return binding;
    }

    public void setBinding(String binding) {
        this.binding = binding;
    }

    public String getCaption_image() {
        return caption_image;
    }

    public void setCaption_image(String caption_image) {
        this.caption_image = caption_image;
    }

    public int getRequired() {
        return required;
    }

    public void setRequired(int required) {
        this.required = required;
    }

    public int getRelevant() {
        return relevant;
    }

    public void setRelevant(int relevant) {
        this.relevant = relevant;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public Object getAnswer() {
        return answer;
    }

    public void setAnswer(Object answer) {
        this.answer = answer;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public HashMap<String, String> getStyle() {
        return style;
    }

    public void setStyle(HashMap<String, String> style) {
        this.style = style;
    }

    public String getCaption_video() {
        return caption_video;
    }

    public void setCaption_video(String caption_video) {
        this.caption_video = caption_video;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCaption_markdown() {
        return caption_markdown;
    }

    public void setCaption_markdown(String caption_markdown) {
        this.caption_markdown = caption_markdown;
    }

    public String getIx() {
        return ix;
    }

    public void setIx(String ix) {
        this.ix = ix;
    }

    public String[] getChoices() {
        return choices;
    }

    public void setChoices(String[] choices) {
        this.choices = choices;
    }

    @Override
    public String toString(){
        String ret= "QuestionBean [ix="+ ix + ", bind=" + this.getBinding() + ", answer=" + this.getAnswer() + ", type: "
                + this.getType() + ", text=" + caption;

        if(choices != null){
            ret += ", choices=" + Arrays.toString(choices);
        }

        if(children != null){
            ret +=", children=" + Arrays.toString(getChildren());
        }

        ret +="]";
        return ret;
    }

    public QuestionBean[] getChildren() {
        return children;
    }

    public void setChildren(QuestionBean[] children) {
        this.children = children;
    }

    public String getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(String repeatable) {
        this.repeatable = repeatable;
    }

    public String getExists() {
        return exists;
    }

    public void setExists(String exists) {
        this.exists = exists;
    }

    @JsonGetter(value = "add-choice")
    public String getAddChoice() {
        return addChoice;
    }
    @JsonSetter(value = "add-choice")
    public void setAddChoice(String addChoice) {
        this.addChoice = addChoice;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public int getControl() {
        return control;
    }

    public void setControl(int control) {
        this.control = control;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }
}
