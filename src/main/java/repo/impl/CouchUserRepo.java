package repo.impl;

import hq.interfaces.CouchUser;
import hq.models.CommCareUser;
import hq.models.WebUser;
import org.lightcouch.CouchDbClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by benrudolph on 9/7/16.
 */
@Repository
public class CouchUserRepo {

    @Autowired
    private CouchDbClient userCouchDbClient;

    public CouchUser getUserByUsername(String username) {
        Class mClass = WebUser.class;
        if (username.endsWith("commcarehq.org")) {
            mClass = CommCareUser.class;
        }
        List<CouchUser> users = userCouchDbClient.view("users/by_username")
                .key(username)
                .includeDocs(true)
                .reduce(false)
                .query(mClass);
        return users.get(0);
    }
}
