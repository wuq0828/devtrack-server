package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VersionDao extends JpaRepository<Version, Long> {

    List<Version> findByProjectIdOrderByCreateTimeDesc(Long projectId);
}
