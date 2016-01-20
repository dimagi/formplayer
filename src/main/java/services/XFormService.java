package services;

import auth.HqAuth;

/**
 * Created by willpride on 1/20/16.
 */
public interface XFormService {
    String getFormXml(String url, HqAuth auth);
}
