package org.commcare.formplayer.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableMap;

import org.commcare.formplayer.application.FormSubmissionController;
import org.commcare.formplayer.application.MenuController;
import org.commcare.formplayer.beans.NewFormResponse;
import org.commcare.formplayer.beans.SessionNavigationBean;
import org.commcare.formplayer.beans.SubmitResponseBean;
import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.beans.menus.CommandListResponseBean;
import org.commcare.formplayer.beans.menus.EntityListResponse;
import org.commcare.formplayer.configuration.CacheConfiguration;
import org.commcare.formplayer.junit.FormSessionTest;
import org.commcare.formplayer.junit.Installer;
import org.commcare.formplayer.junit.RestoreFactoryExtension;
import org.commcare.formplayer.junit.StorageFactoryExtension;
import org.commcare.formplayer.junit.request.SessionNavigationRequest;
import org.commcare.formplayer.junit.request.SubmitFormRequest;
import org.commcare.formplayer.utils.TestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({MenuController.class, FormSubmissionController.class})
@ContextConfiguration(classes={TestContext.class, CacheConfiguration.class})
@FormSessionTest
public class BackNavigationTests {

    @Autowired
    private MockMvc mockMvc;

    @RegisterExtension
    static RestoreFactoryExtension restoreFactoryExt = new RestoreFactoryExtension.builder()
            .withUser("back_nav").withDomain("back_nav")
            .withRestorePath("restores/basic.xml")
            .build();

    @RegisterExtension
    static StorageFactoryExtension storageExt = new StorageFactoryExtension.builder()
            .withUser("back_nav").withDomain("back_nav").build();

    @Test
    public void testGoBackToFormListForModule() throws Exception {
        // navigate to 'Case Tests' -> 'Create a Case' form
        String[] selections = {"2", "0"};

        String sessionId = navigate(selections, NewFormResponse.class, null).getSessionId();

        SubmitResponseBean submitResponseBean = new SubmitFormRequest(mockMvc).request(
                sessionId, ImmutableMap.of("0", "name", "1", "1"), true
        ).bean();
        assertEquals("success", submitResponseBean.getStatus());

        // performing the same navigation again (but with the sessionId) should put us back at the
        // form list screen for the 'Case Tests' (m2) module
        CommandListResponseBean backResponse = navigate(selections, CommandListResponseBean.class, sessionId);
        assertThat(backResponse.getCommands())
                .hasSize(6)
                .anyMatch(command -> command.getIndex() == 0 && command.getDisplayText().equals("Create a Case"));
    }

    @Test
    public void testGoBackToCaseList() throws Exception {
        // navigate to 'Case Tests' -> 'Update a Case' form with 'c3' case
        String[] selections = {"2", "1", "124938b2-c228-4107-b7e6-31a905c3f4ff"};

        String sessionId = navigate(selections, NewFormResponse.class, null).getSessionId();

        SubmitResponseBean submitResponseBean = new SubmitFormRequest(mockMvc).request(
                sessionId, ImmutableMap.of("4", 1 ), true
        ).bean();
        assertEquals("success", submitResponseBean.getStatus());

        // performing the same navigation again (but with the sessionId) should put us back at the
        // case list screen
        EntityListResponse backResponse = navigate(selections, EntityListResponse.class, sessionId);
        assertThat(backResponse.getTitle()).isEqualTo("Update a Case");
    }

    private <T extends BaseResponseBean> T navigate(String[] selections, Class<T> responseClass, String sessionId) throws Exception {
        String installReference = Installer.getInstallReference("basic");
        SessionNavigationRequest<T> request = new SessionNavigationRequest<>(mockMvc, responseClass, installReference);
        SessionNavigationBean bean = request.getNavigationBean(selections);
        if (sessionId != null) {
            bean.setFormSessionId(sessionId);
        }
        return request.requestWithBean(bean).bean();
    }

}
