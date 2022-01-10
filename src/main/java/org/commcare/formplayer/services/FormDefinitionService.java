package org.commcare.formplayer.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.commcare.formplayer.exceptions.MultipleFormDefsFoundException;
import org.commcare.formplayer.objects.FormDefinition;
import org.commcare.formplayer.repo.FormDefinitionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@CacheConfig(cacheNames = {"form_definition"})
public class FormDefinitionService {

    private final Log log = LogFactory.getLog(FormDefinitionService.class);

    @Autowired
    private FormDefinitionRepo formDefinitionRepo;


    @Cacheable(key="{#appId, #appVersion, #formXmlns}")
    public FormDefinition getOrCreateFormDefinition(String appId, String appVersion, String formXmlns, String serializedFormDef) {
        Optional<FormDefinition> optFormDef = this.formDefinitionRepo.findByAppIdAndAppVersionAndXmlns(appId, appVersion, formXmlns);
        return optFormDef.orElseGet(() -> {
            FormDefinition newFormDef = new FormDefinition(appId, appVersion, formXmlns, serializedFormDef);
            return this.formDefinitionRepo.save(newFormDef);
        });
    }
}
