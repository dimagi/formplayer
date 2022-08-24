package org.commcare.formplayer.auth;

import static org.commcare.formplayer.util.Constants.PART_ANSWER;
import static org.commcare.formplayer.util.Constants.PART_FILE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.commcare.formplayer.util.Constants;
import org.commcare.formplayer.utils.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Common methods for session auth and hmac auth tests
 */
public class AuthTestUtils {

    /**
     * Use the 'clear_user_data' endpoint for 'full auth' which required user details.
     */
    public static MockHttpServletRequestBuilder getFullAuthRequestBuilder(String body) {
        return post(String.format("/%s", Constants.URL_CLEAR_USER_DATA))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.csrf());
    }

    /**
     * Use the 'answer_media' endpoint for 'full auth' with multipart
     */
    public static MockHttpServletRequestBuilder getMultipartRequestBuilder(Class c, String body) throws IOException {
        InputStream fis = FileUtils.getFileStream(c, "media/valid_image.jpg");
        MockMultipartFile file = new MockMultipartFile(PART_FILE, "valid_image.jpg", MediaType.IMAGE_JPEG_VALUE, fis);
        MockPart answer = new MockPart(PART_ANSWER, body.getBytes());
        answer.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return  multipart(String.format("/%s", Constants.URL_ANSWER_MEDIA_QUESTION))
                .file(file)
                .part(answer)
                .with(SecurityMockMvcRequestPostProcessors.csrf());
    }
}
