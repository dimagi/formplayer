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

import java.util.List;

@Service
@CacheConfig(cacheNames = {"form_definition"})
public class FormDefinitionService {

    private final Log log = LogFactory.getLog(FormDefinitionService.class);

    @Autowired
    private FormDefinitionRepo formDefinitionRepo;


    @Cacheable
    public FormDefinition getOrCreateFormDefinition(String appId, String appVersion, String formXmlns, String serializedFormDef) {
        List<FormDefinition> formDefs = this.formDefinitionRepo.findByAppIdAndAppVersionAndXmlns(appId, appVersion, formXmlns);
        FormDefinition formDefinition;
        if (formDefs.size() > 1) {
            throw new MultipleFormDefsFoundException(appId, appVersion, formXmlns);
        } else if (formDefs.size() == 1) {
            formDefinition = formDefs.get(0);
        } else {
            formDefinition = new FormDefinition(formXmlns, appVersion, appId, serializedFormDef);
            this.formDefinitionRepo.save(formDefinition);
        }
        return formDefinition;
    }
}
