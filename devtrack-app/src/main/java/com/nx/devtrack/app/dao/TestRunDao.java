package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRunDao extends JpaRepository<TestRun, Long> {

    List<TestRun> findByProjectIdOrderByCreateTimeDesc(Long projectId);
}
