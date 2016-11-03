package services;

import auth.HqAuth;

/**
 * Created by benrudolph on 11/3/16.
 */
public interface FormattedQuestionsService {
    String getFormattedQuestions(String domain, String appId, String xmlns, String instanceXml, HqAuth auth);
}
