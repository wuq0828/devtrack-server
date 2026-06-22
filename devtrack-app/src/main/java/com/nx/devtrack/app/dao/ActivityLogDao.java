package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogDao extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByEntityTypeAndEntityIdOrderByCreateTimeDesc(String entityType, Long entityId);

    List<ActivityLog> findTop20ByOrderByCreateTimeDesc();
}
