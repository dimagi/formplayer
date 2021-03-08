package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.FormSessionListView;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormSessionRepo extends JpaRepository<SerializableFormSession, String> {
    List<FormSessionListView> findByUsernameAndDomainAndAsUserOrderByDateCreatedDesc(String username, String domain, String asUser);
    List<FormSessionListView> findByUsernameAndDomainAndAsUserIsNullOrderByDateCreatedDesc(String username, String domain);
}
