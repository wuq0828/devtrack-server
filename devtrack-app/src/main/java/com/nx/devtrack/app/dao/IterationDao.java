package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.Iteration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IterationDao extends JpaRepository<Iteration, Long> {

    List<Iteration> findByProjectIdOrderByCreateTimeDesc(Long projectId);
}
