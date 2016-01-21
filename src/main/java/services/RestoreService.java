package services;

import auth.HqAuth;

/**
 * Created by willpride on 1/21/16.
 */
public interface RestoreService {
    String getRestoreXml(String host, String domain, HqAuth auth);
}
