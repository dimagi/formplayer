package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.SerializableMenuSession;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by willpride on 8/1/16.
 */
public interface MenuSessionRepo extends JpaRepository<SerializableMenuSession, String> {
}
