package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.ObservLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservLogDao extends JpaRepository<ObservLog, Long> {

    List<ObservLog> findByProjectIdOrderByCreateTimeDesc(Long projectId);

    List<ObservLog> findByProjectIdAndMessageContainingOrderByCreateTimeDesc(Long projectId, String keyword);
}
