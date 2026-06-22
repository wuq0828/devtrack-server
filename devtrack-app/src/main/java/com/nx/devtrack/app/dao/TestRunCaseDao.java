package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.TestRunCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRunCaseDao extends JpaRepository<TestRunCase, Long> {

    List<TestRunCase> findByRunIdOrderByCreateTimeAsc(Long runId);

    TestRunCase findByRunIdAndTestCaseId(Long runId, Long testCaseId);
}
