package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.EntityTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntityTagDao extends JpaRepository<EntityTag, Long> {

    List<EntityTag> findByEntityTypeAndEntityId(String entityType, Long entityId);
}
