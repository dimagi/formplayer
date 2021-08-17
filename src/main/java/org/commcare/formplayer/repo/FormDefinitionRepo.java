package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.FormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormDefinitionRepo extends JpaRepository<FormDefinition, String> {
    List<FormDefinition> findByAppIdAndAppVersionAndXmlns(String appId, String appVersion, String xmlns);
}

