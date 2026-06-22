package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseDao extends JpaRepository<TestCase, Long> {

    List<TestCase> findByProjectIdOrderByCreateTimeDesc(Long projectId);

    List<TestCase> findByProjectIdAndRegressionTrueOrderByCreateTimeAsc(Long projectId);

    List<TestCase> findByIdIn(List<Long> ids);
}
