package org.commcare.formplayer.utils;

import org.hibernate.proxy.HibernateProxy;

public class JpaTestUtils {

    public static <T> T unwrapProxy(T entity) {
        if (entity instanceof HibernateProxy) {
            return (T) ((HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
        }
        return null;
    }
}
