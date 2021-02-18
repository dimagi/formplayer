package org.commcare.formplayer.utils;

import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import java.util.Map;

public class JpaTestUtils {

    private static final ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

    public static <T> T unwrapProxy(T entity) {
        if (entity instanceof HibernateProxy) {
            return (T) ((HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
        }
        return null;
    }

    public static <T> T createProjection(Class<T> projectionClass, Map<String, Object> backingMap) {
        return factory.createProjection(projectionClass, backingMap);
    }
}
