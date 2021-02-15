package org.commcare.formplayer.objects;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Entity
@Table(name="formplayer_sessions")
@EntityListeners(AuditingEntityListener.class)
public class SerializableFormSession implements Serializable{
    @Getter
    @Id
    @GeneratedValue( generator="uuid" )
    @GenericGenerator(name="uuid", strategy="org.hibernate.id.UUIDGenerator")
    private String id;

    @Getter
    @Version
    private int version;

    /**
     * Deprecated: to be removed once `dateCreated` is fully populated
     */
    @Getter
    @Column(name="dateopened", updatable=false)
    private String dateOpened;

    @Getter
    @CreatedDate
    @Column(name="datecreated")
    private Instant dateCreated;

    @Getter
    @Column(updatable=false)
    private String domain;

    @Getter
    @Column(name="asuser", updatable=false)
    private String asUser;

    @Getter
    @Column(name="appid", updatable=false)
    private String appId;

    @Getter
    @Column(name="caseid", updatable=false)
    private String restoreAsCaseId;

    @Getter
    @Column(name="posturl", updatable=false)
    private String postUrl;

    @Getter
    @Column(name="menu_session_id", updatable=false)
    private String menuSessionId;

    @Getter
    @Column(updatable=false)
    private String title;

    @Getter
    @Column(name="onequestionperscreen", updatable=false)
    private boolean oneQuestionPerScreen;

    @Getter
    @Setter
    @Column(name="formxml", updatable=false)
    private String formXml;

    @Getter
    @Setter
    @Column(name="instancexml")
    private String instanceXml;

    @Getter
    @Column(updatable=false)
    private String username;

    @Getter
    @Setter
    @Column(name="initlang")
    private String initLang;

    /**
     * Deprecated. To be replaced by ``version``
     */
    @Getter
    @Column(name="sequenceid")
    @Convert(converter=IntStringConverter.class)
    private Integer sequenceId;

    @Getter
    @Column(name="sessiondata")
    @Convert(converter=ByteArrayConverter.class)
    private Map<String, String> sessionData;

    @Getter
    @Setter
    @Column(name="currentindex")
    private String currentIndex;

    @Getter
    @Column(name="functioncontext")
    @Convert(converter=ByteArrayConverter.class)
    private Map<String, FunctionHandler[]> functionContext;

    @Getter
    @Column(name="inpromptmode")
    private boolean inPromptMode;

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
        this.dateOpened = new Date().toString();
        this.currentIndex = "0";
    }

    public void incrementSequence() {
        if (sequenceId == null) {
            sequenceId = 0;
        } else {
            sequenceId += 1;
        }
    }

    @Override
    public String toString(){
        return "Session [id=" + id + ", sequence=" + sequenceId + ", username=" + username
                + " domain=" + domain + ", instance=" + instanceXml + "]";
    }
}
