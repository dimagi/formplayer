package org.commcare.formplayer.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
import org.commcare.formplayer.junit.request.Response;
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
@ContextConfiguration(classes = {TestContext.class, CacheConfiguration.class})
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
    public void testGoBackToCaseList() throws Exception {
        // navigate to 'Case Tests' -> 'Update a Case' -> [c3]
        String[] selections = {"2", "1", "124938b2-c228-4107-b7e6-31a905c3f4ff"};

        // check navigation is correct
        navigate(selections, NewFormResponse.class, null)
                .andExpect(jsonPath("title").value("Update a Case"))
                .andExpect(jsonPath(
                        "breadcrumbs",
                        containsInRelativeOrder("Basic Tests", "Case Tests", "Update a Case", "c3")
                ));

        // performing the same navigation again (but with the sessionId) should put us back at the
        // case list screen
        EntityListResponse backResponse = navigate(selections, EntityListResponse.class, "123").bean();
        assertThat(backResponse.getTitle()).isEqualTo("Update a Case");
    }

    @Test
    public void testGoBackToCaseFormList() throws Exception {
        // navigate to 'Minimize Duplicates' -> [c3] -> 'Update a Case'
        String[] selections = {"5", "124938b2-c228-4107-b7e6-31a905c3f4ff", "0"};

        // check navigation is correct
        navigate(selections, NewFormResponse.class, null)
                .andExpect(jsonPath("title").value("Update a Case"))
                .andExpect(jsonPath(
                        "breadcrumbs",
                        containsInRelativeOrder("Basic Tests", "Minimize Duplicates", "c3", "Update a Case")
                ));

        // performing the same navigation again (but with the sessionId) should put us back at the
        // case list screen (since the selected case is now closed)
        CommandListResponseBean backResponse = navigate(selections, CommandListResponseBean.class, "123").bean();
        assertThat(backResponse.getTitle()).isEqualTo("Minimize Duplicates");
        assertThat(backResponse.getCommands()).hasSize(2)
                .extracting("displayText")
                .containsExactly("Update a Case", "Close a Case");
    }

    @Test
    public void testGoBackToCaseListAfterCaseClosed() throws Exception {
        // navigate to 'Minimize Duplicates' -> [c3] -> 'Close a Case'
        String[] selections = {"5", "124938b2-c228-4107-b7e6-31a905c3f4ff", "1"};

        Response<NewFormResponse> formResponse = navigate(selections, NewFormResponse.class, null);
        formResponse.andExpect(jsonPath("title").value("Close a Case"))
                .andExpect(jsonPath(
                        "breadcrumbs",
                        containsInRelativeOrder("Basic Tests", "Minimize Duplicates", "c3", "Close a Case")
                ));
        String sessionId = formResponse.bean().getSessionId();

        SubmitResponseBean submitResponseBean = new SubmitFormRequest(mockMvc).request(
                sessionId, ImmutableMap.of("0", "1"), true
        ).bean();
        assertThat(submitResponseBean.getStatus()).isEqualTo("success");

        // performing the same navigation again (but with the sessionId) should put us back at the
        // case list screen (since the selected case is now closed)
        EntityListResponse backResponse = navigate(selections, EntityListResponse.class, "123").bean();
        assertThat(backResponse.getTitle()).isEqualTo("Minimize Duplicates");
    }

    private <T extends BaseResponseBean> Response<T> navigate(
            String[] selections, Class<T> responseClass, String sessionId) {
        String installReference = Installer.getInstallReference("basic");
        SessionNavigationRequest<T> request = new SessionNavigationRequest<>(
                mockMvc, responseClass, installReference);
        SessionNavigationBean bean = request.getNavigationBean(selections);
        if (sessionId != null) {
            bean.setFormSessionId(sessionId);
        }
        return request.requestWithBean(bean);
    }
}
