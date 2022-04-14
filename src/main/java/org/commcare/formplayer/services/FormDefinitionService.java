package org.commcare.formplayer.services;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.objects.SerializableFormDefinition;
import org.commcare.formplayer.repo.FormDefinitionRepo;
import org.commcare.formplayer.util.serializer.FormDefStringSerializer;
import org.javarosa.core.log.WrappedException;
import org.javarosa.core.model.FormDef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Service class that abstracts interactions with FormDefinitionRepo
 */
@Service
@CacheConfig(cacheNames = {"form_definition"})
public class FormDefinitionService {

    private final Log log = LogFactory.getLog(FormDefinitionService.class);

    @Autowired
    private FormDefinitionRepo formDefinitionRepo;

    /**
     * Checks if an entry for this (appId, formXmlns, formVersion) combination already exists, and returns if so
     * Otherwise creates a new entry which entails serializing the formDef object (costly operation)
     *
     * @param appId id for application built in HQ
     * @param formXmlns xmlns identifier for specific form within app
     * @param formVersion version of form xml
     * @param formDef FormDef to serialize and save to SQL if needed
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
        SerializableFormDefinition formDefinition = optFormDef.orElseGet(() -> {
            try {
                String serializedFormDef = FormDefStringSerializer.serialize(formDef);
                SerializableFormDefinition newFormDef = new SerializableFormDefinition(
                        appId, formXmlns, formVersion, serializedFormDef
                );
                return this.formDefinitionRepo.save(newFormDef);
            } catch (IOException e) {
                throw new WrappedException("Error serializing form def", e);
            }
        });
        formDefinition.setFormDef(formDef);
        return formDefinition;
    }
}
