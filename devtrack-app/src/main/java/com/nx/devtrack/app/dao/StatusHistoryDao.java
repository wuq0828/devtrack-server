package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusHistoryDao extends JpaRepository<StatusHistory, Long> {

    List<StatusHistory> findByEntityTypeAndEntityIdOrderByCreateTimeAsc(String entityType, Long entityId);

    StatusHistory findFirstByEntityTypeAndEntityIdOrderByCreateTimeDesc(String entityType, Long entityId);
}
