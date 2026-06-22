package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.Relation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelationDao extends JpaRepository<Relation, Long> {

    List<Relation> findBySourceTypeAndSourceId(String sourceType, Long sourceId);

    List<Relation> findByTargetTypeAndTargetId(String targetType, Long targetId);

    Relation findBySourceTypeAndSourceIdAndTargetTypeAndTargetIdAndRelationType(
            String sourceType, Long sourceId, String targetType, Long targetId, String relationType);
}
