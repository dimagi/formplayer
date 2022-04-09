package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.EntitiesSelection;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JpaRepository implementation for 'entities_selection' entity
 */
public interface EntitiesSelectionRepo extends JpaRepository<EntitiesSelection, String> {
}
