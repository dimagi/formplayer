package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.SerializableFormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Abstracted layer for SerializableFormDefinition database operations
 */
public interface FormDefinitionRepo extends JpaRepository<SerializableFormDefinition, Long> {
    Optional<SerializableFormDefinition> findByAppIdAndFormXmlnsAndFormVersion(String appId,
                                                                               String formXmlns,
                                                                               String formVersion);
}

