package repo;

import objects.SerializableFormSession;

import java.util.List;

/**
 * Created by willpride on 1/19/16.
 */
public interface TokenRepo {
    boolean isAuthorized(String token);
}
