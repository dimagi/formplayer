package org.commcare.formplayer.services;

import org.commcare.formplayer.objects.SerializableFormDefinition;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.commcare.formplayer.repo.FormDefinitionRepo;
import org.commcare.formplayer.session.FormSession;
import org.commcare.formplayer.util.serializer.FormDefStringSerializer;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.services.storage.IStorageUtilityIndexed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Service class that abstracts interactions with FormDefinitionRepo
 */
@Service
@CacheConfig(cacheNames = {"form_definition"})
public class FormDefinitionService {

    @Autowired
    private FormDefinitionRepo formDefinitionRepo;

    @Autowired
    private FormplayerStorageFactory storageFactory;

    @Autowired
    private CacheManager caches;

    /**
     * Checks if an entry for this (appId, formXmlns, formVersion) combination already exists, and returns if so
     * Otherwise creates a new entry which entails serializing the formDef object (costly operation)
     *
     * @param appId       id for application built in HQ
     * @param formXmlns   xmlns identifier for specific form within app
     * @param formVersion version of form xml
     * @param formDef     FormDef to serialize and save to SQL if needed
     * @return already existing or newly created SerializableFormDefinition
     */
    @Cacheable(key = "{#appId, #formXmlns, #formVersion}")
    public SerializableFormDefinition getOrCreateFormDefinition(
            String appId,
            String formXmlns,
            String formVersion,
            FormDef formDef) {
        Optional<SerializableFormDefinition> optFormDef = this.formDefinitionRepo
                .findByAppIdAndFormXmlnsAndFormVersion(appId, formXmlns, formVersion);
        return optFormDef.orElseGet(() -> {
            String serializedFormDef;
            try {
                serializedFormDef = FormDefStringSerializer.serialize(formDef);
            } catch (IOException e) {
                throw new WrappedException("Error serializing form def", e);
            }
            SerializableFormDefinition newFormDef = new SerializableFormDefinition(
                    appId, formXmlns, formVersion, serializedFormDef
            );
            return this.formDefinitionRepo.save(newFormDef);
        });
    }


    private SerializableFormDefinition updateFormDefinition(
            SerializableFormDefinition formDefinition,
            FormDef formDef) {
        String serializedFormDef;
        try {
            serializedFormDef = FormDefStringSerializer.serialize(formDef);
        } catch (IOException e) {
            throw new WrappedException("Error serializing form def", e);
        }
        formDefinition.setSerializedFormDef(serializedFormDef);
        return this.formDefinitionRepo.save(formDefinition);
    }

    /**
     * Ensure raw xml is accessible locally in case serialization changes which would break the ability to
     * deserialize the serialized form def in postgres
     *
     * @return True if the value was written to storage or False if it already exists in storage.
     */
    public boolean writeToLocalStorage(FormDef formDef) {
        this.storageFactory.getStorageManager().registerStorage(FormDef.STORAGE_KEY, FormDef.class);
        String xmlns = formDef.getMainInstance().schema;
        if (getFormDefFromStorage(xmlns).isPresent()) {
            return false;
        }
        writeFormDefToStorage(formDef);
        return true;
    }

    /**
     * This method uses the sessionId to cache deserialized FormDefs
     * Deserializing a serialized FormDef object is costly, and this avoids doing so in between requests
     * within the same session
     *
     * @param session session that contains session id and serialized formDef
     * @return deserialized FormDef object
     */
    public FormDef getFormDef(SerializableFormSession session) {
        FormDef formDef = this.getFormDefWithCache(session);
        // ensure previous tree references are cleared (only necessary when retrieving from cache)
        formDef.getMainInstance().cleanCache();
        return formDef;
    }

    /**
     * Always use public getFormDef to ensure internal FormDef cached references are cleared.
     *
     * We can't use @{@link Cacheable} here since we want to keep this method private.
     */
    private FormDef getFormDefWithCache(SerializableFormSession session) {
        Cache cache = caches.getCache("form_definition");
        return cache.get(session.getId(), () -> getFormDefFromSession(session));
    }

    private FormDef getFormDefFromSession(SerializableFormSession session) {
        SerializableFormDefinition formDefinition = session.getFormDefinition();
        try {
            return FormDefStringSerializer.deserialize(formDefinition.getSerializedFormDef());
        } catch (Exception e) {
            String xmlns = formDefinition.getFormXmlns();
            FormDef formDef = getFormDefFromStorage(xmlns).orElseThrow(() -> {
                return new WrappedException("Unable to load form def after serialization error", e);
            });
            this.updateFormDefinition(formDefinition, formDef);
            return formDef;
        }
    }

    /**
     * Cache the form def for future requests in this session
     *
     * @param session grab the id to cache on and the deserialized form def from the session
     * @return deserialized FormDef object
     */
    @CachePut(key = "#session.getSessionId()")
    public FormDef cacheFormDef(FormSession session) {
        return session.getFormDef();
    }

    private Optional<FormDef> getFormDefFromStorage(String xmlns) {
        this.storageFactory.getStorageManager().registerStorage(FormDef.STORAGE_KEY, FormDef.class);
        try {
            return Optional.of(getFormDefStorage().getRecordForValue("XMLNS", xmlns));
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    private void writeFormDefToStorage(FormDef formDef) {
        getFormDefStorage().write(formDef);
    }

    private IStorageUtilityIndexed<FormDef> getFormDefStorage() {
        return this.storageFactory.getStorageManager().getStorage(FormDef.STORAGE_KEY);
    }

}
