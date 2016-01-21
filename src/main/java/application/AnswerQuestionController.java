package application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RestController;
import repo.SessionRepo;
import services.XFormService;

/**
 * Created by willpride on 1/20/16.
 */
@RestController
@EnableAutoConfiguration
public class AnswerQuestionController {

    @Autowired
    private SessionRepo sessionRepo;

    @Autowired
    private XFormService xFormService;

}
