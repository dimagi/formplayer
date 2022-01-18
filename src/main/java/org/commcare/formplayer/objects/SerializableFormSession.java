package org.commcare.formplayer.objects;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name="formplayer_sessions")
@EntityListeners(AuditingEntityListener.class)
@Getter
public class SerializableFormSession implements Serializable{
    public enum SubmitStatus {
        PROCESSED_XML,
        PROCESSED_STACK
    }

    @Id
    @GeneratedValue( generator="uuid" )
    @GenericGenerator(name="uuid", strategy="org.hibernate.id.UUIDGenerator")
    private String id;

    @Version
    private int version;

    @CreatedDate
    @Column(name="datecreated")
    private Instant dateCreated;

    @Column(updatable=false)
    private String domain;

    @Column(name="asuser", updatable=false)
    private String asUser;

    @Column(name="appid", updatable=false)
    private String appId;

    @Column(name="caseid", updatable=false)
    private String restoreAsCaseId;

    @Column(name="posturl", updatable=false)
    private String postUrl;

    @Column(name="menu_session_id", updatable=false)
    private String menuSessionId;

    @Column(updatable=false)
    private String title;

    @Column(name="onequestionperscreen", updatable=false)
    private boolean oneQuestionPerScreen;

    @Setter
    @Column(name="formxml", updatable=false)
    private String formXml;

    @Setter
    @Column(name="instancexml")
    private String instanceXml;

    @Setter
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="form_definition_id")
    private SerializableFormDefinition formDefinition;

    @Column(updatable=false)
    private String username;

    @Setter
    @Column(name="initlang")
    private String initLang;

    @Column(name="sessiondata")
    @Convert(converter=ByteArrayConverter.class)
    private Map<String, String> sessionData;

    @Setter
    @Column(name="currentindex")
    private String currentIndex;

    @Column(name="functioncontext")
    @Convert(converter=ByteArrayConverter.class)
    private Map<String, FunctionHandler[]> functionContext;

    @Column(name="inpromptmode")
    private boolean inPromptMode;

    private String submitStatus;

    public SerializableFormSession() { }
    public SerializableFormSession(String id) {
        this.id = id;
    }

    public SerializableFormSession(
            String domain,
            String appId,
            String username,
            String asUser,
            String restoreAsCaseId,
            String postUrl,
            String menuSessionId,
            String title,
            boolean oneQuestionPerScreen,
            String initLang,
            boolean inPromptMode,
            Map<String, String> sessionData,
            Map<String, FunctionHandler[]> functionContext) {
        this.domain = domain;
        this.asUser = asUser;
        this.appId = appId;
        this.restoreAsCaseId = restoreAsCaseId;
        this.postUrl = postUrl;
        this.menuSessionId = menuSessionId;
        this.title = title;
        this.oneQuestionPerScreen = oneQuestionPerScreen;
        this.username = username;
        this.initLang = initLang;
        this.sessionData = sessionData;
        this.functionContext = functionContext;
        this.inPromptMode = inPromptMode;
        this.currentIndex = "0";
    }

    public void setSubmitStatus(SubmitStatus submitStatus) {
        this.submitStatus = submitStatus.name();
    }

    public SubmitStatus getSubmitStatus() {
        return submitStatus == null ? null : SubmitStatus.valueOf(submitStatus);
    }

    public boolean isProcessingStageComplete(SubmitStatus stage) {
        SubmitStatus status = getSubmitStatus();
        return status != null && status.compareTo(stage) >= 0;
    }

    @Override
    public String toString(){
        return "Session [id=" + id + ", version=" + version + ", username=" + username
                + " domain=" + domain + ", instance=" + instanceXml
                + ", submitStatus=" + submitStatus + "]";
    }
}
