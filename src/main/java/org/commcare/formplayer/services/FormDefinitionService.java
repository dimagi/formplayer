package org.commcare.formplayer.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.objects.SerializableFormDefinition;
import org.commcare.formplayer.repo.FormDefinitionRepo;
import org.commcare.formplayer.util.serializer.FormDefStringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import org.javarosa.core.model.FormDef;
import org.javarosa.core.log.WrappedException;

import java.io.IOException;
import java.util.Optional;

@Service
@CacheConfig(cacheNames = {"form_definition"})
public class FormDefinitionService {

    private final Log log = LogFactory.getLog(FormDefinitionService.class);

    @Autowired
    private FormDefinitionRepo formDefinitionRepo;


    @Cacheable(key="{#appId, #formXmlns, #formVersion}")
    public SerializableFormDefinition getOrCreateFormDefinition(String appId, String formXmlns, String formVersion, FormDef formDef) {
        Optional<SerializableFormDefinition> optFormDef = this.formDefinitionRepo.findByAppIdAndFormXmlnsAndFormVersion(appId, formXmlns, formVersion);
        return optFormDef.orElseGet(() -> {
            try {
                String serializedFormDef = FormDefStringSerializer.serialize(formDef);
                SerializableFormDefinition newFormDef = new SerializableFormDefinition(appId, formXmlns, formVersion, serializedFormDef);
                return this.formDefinitionRepo.save(newFormDef);
            } catch (IOException e) {
                throw new WrappedException("Error serializing form def", e);
            }
        });
    }
}
