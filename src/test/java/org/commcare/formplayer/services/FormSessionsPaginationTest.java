package org.commcare.formplayer.services;

import com.google.common.collect.ImmutableMap;

import org.commcare.formplayer.beans.FormsSessionsRequestBean;
import org.commcare.formplayer.objects.FormSessionListView;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.repo.FormSessionRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

@ExtendWith(SpringExtension.class)
@ContextConfiguration
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableJpaAuditing
public class FormSessionsPaginationTest {

    @Autowired
    FormSessionService formSessionService;

    @Autowired
    private FormSessionRepo formSessionRepo;

    @Test
    public void testPagination() {
        int totalNumberOfForms = 18;
        for (int i = 0; i < totalNumberOfForms; i++) {
            SerializableFormSession session = new SerializableFormSession(
                    "domain", "appId", "momo", "momo", "restoreAsCaseId",
                    "/a/domain/receiver", null, "More momo", true, "en", false,
                    ImmutableMap.of("a", "1", "b", "2"),
                    null
            );
            formSessionService.saveSession(new SerializableFormSession("randomformid" + i));
        }
        FormsSessionsRequestBean formsSessionsRequestBean = new FormsSessionsRequestBean();
        formsSessionsRequestBean.setUsername("momo");
        formsSessionsRequestBean.setDomain("domain");
        formsSessionsRequestBean.setRestoreAs("momo");
        formsSessionsRequestBean.setPageSize(5);
        formsSessionsRequestBean.setOffset(15);
        List<FormSessionListView> formSessions = formSessionService.getSessionsForUser("momo", formsSessionsRequestBean);
        assert formSessions.size() == 3;
        formSessionService.purge();
    }

    // only include the service under test and it's dependencies
    // This should not be necessary but we're using older versions of junit and spring
    @ComponentScan(
            basePackageClasses = {FormSessionService.class},
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {FormSessionService.class})
    )
    @EnableCaching
    @Configuration
    public static class FormSessionServiceTestConfig {

//        @Bean
//        public CacheManager cacheManager() {
//            return new ConcurrentMapCacheManager("form_session");
//        }
    }
}
