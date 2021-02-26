package org.commcare.formplayer.repo;

import org.hibernate.Session;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;

/**
 * JPA repository implementation that uses ``Session.update`` instead of
 * ``Session.merge`` when saving. This avoids the additional 'select' that
 * happens when saving detached entities.
 *
 * Formplayer caches entities between requests to avoid having to re-fetch them
 * from the DB on successive requests. Since the caching results in detached
 * entities, the advantage of caching is lost when using ``Session.merge`` since
 * the merge will re-fetch the entity from the DB on update.
 *
 * It is safe to skip the select before update as long as the Formplayer
 * request routing is consistent or the caching is global across all Formplayer instances.
 */
public class FormplayerBaseJpaRepoImpl<T, ID>
        extends SimpleJpaRepository<T, ID> {

    private final EntityManager entityManager;
    private final JpaEntityInformation<T, ?> entityInformation;

    public FormplayerBaseJpaRepoImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#save(java.lang.Object)
     */
    @Transactional
    @Override
    public <S extends T> S save(S entity) {
        Assert.notNull(entity, "Entity must not be null.");

        if (entityInformation.isNew(entity)) {
            entityManager.persist(entity);
            return entity;
        } else {
            entityManager.unwrap(Session.class).update(entity);
            return entity;
        }
    }
}
