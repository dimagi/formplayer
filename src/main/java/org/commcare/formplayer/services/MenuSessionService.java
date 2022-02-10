package org.commcare.formplayer.services;

import org.commcare.formplayer.exceptions.MenuNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.commcare.formplayer.repo.MenuSessionRepo;
import org.commcare.formplayer.objects.SerializableMenuSession;
import org.commcare.formplayer.util.Constants;

import javax.persistence.LockModeType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;


@Service
@CacheConfig(cacheNames = {"menu_session"})
public class MenuSessionService {

    @Autowired
    private MenuSessionRepo menuSessionRepo;

    @Cacheable
    public SerializableMenuSession getSessionById(String id) {
        Optional<SerializableMenuSession> session = menuSessionRepo.findById(id);
        if (!session.isPresent()) {
            throw new MenuNotFoundException(id);
        }
        return session.get();
    }


    @CachePut(key = "#session.id")
    public SerializableMenuSession saveSession(SerializableMenuSession session) {
        return menuSessionRepo.save(session);
    }

}
