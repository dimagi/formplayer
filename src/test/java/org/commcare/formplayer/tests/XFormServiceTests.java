package org.commcare.formplayer.tests;

import org.commcare.formplayer.services.RestoreFactory;
import org.commcare.formplayer.services.XFormService;
import org.commcare.formplayer.utils.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@AutoConfigureWebClient(registerRestTemplate = true)
@RestClientTest({XFormService.class})
/**
 * Regressions for the submission service processing
 */
public class XFormServiceTests {

    @MockBean
    RestoreFactory restoreFactory;

    @Autowired
    private XFormService service;

    @Autowired
    private MockRestServiceServer server;
    @Before
    public void setUp() throws Exception {
        this.server.reset();
    }

    @Test
    public void assertResponse() {
        String requestPath = "https://formplayer.test/xform/request/xforms/oqps.xml";

        this.server.expect(requestTo(requestPath)).andRespond(withSuccess(FileUtils.getFile(this.getClass(), "xforms/oqps.xml"), null));

        this.service.getFormXml(requestPath);
    }
}