package org.commcare.formplayer.repo;

import org.commcare.formplayer.objects.FormSessionListDetailsView;
import org.commcare.formplayer.objects.SerializableFormSession;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FormSessionRepo extends JpaRepository<SerializableFormSession, String> {
    List<FormSessionListDetailsView> findByUsername(String username, Sort sort);
}
