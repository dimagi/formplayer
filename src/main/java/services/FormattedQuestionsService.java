package services;

import auth.HqAuth;

/**
 * Service that gets HTML formatted questions to display to the user
 */
public interface FormattedQuestionsService {
    String getFormattedQuestions(String domain, String appId, String xmlns, String instanceXml, HqAuth auth);
}
