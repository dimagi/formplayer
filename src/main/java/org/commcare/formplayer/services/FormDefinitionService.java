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
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    /**
     * Ensure raw xml is accessible locally in case serialization changes which would break the ability to
     * deserialize the serialized form def in postgres
     */
    public void writeToLocalStorage(FormDef formDef) {
        IStorageUtilityIndexed<FormDef> formStorage = this.getFormDefStorage();
        String xmlns = formDef.getMainInstance().schema;
        if (formStorage.getRecordForValue("XMLNS", xmlns) == null) {
            formStorage.write(formDef);
        }
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
        FormDef formDef = this.internalGetFormDef(session);
        // ensure previous tree references are cleared (only necessary when retrieving from cache)
        formDef.getMainInstance().cleanCache();
        return formDef;
    }

    /**
     * Always use public getFormDef to ensure internal FormDef cached references are cleared
     */
    @Cacheable(key = "#session.id")
    private FormDef internalGetFormDef(SerializableFormSession session) {
        FormDef formDef;
        try {
            formDef = FormDefStringSerializer.deserialize(session.getFormDefinition().getSerializedFormDef());
        } catch (Exception e) {
            String xmlns = session.getFormDefinition().getFormXmlns();
            formDef = this.getFormDefStorage().getRecordForValue(FormDef.STORAGE_KEY, xmlns);
        }
        return formDef;
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

    private IStorageUtilityIndexed<FormDef> getFormDefStorage() {
        return this.storageFactory.getStorageManager().getStorage(FormDef.STORAGE_KEY);
    }
}
