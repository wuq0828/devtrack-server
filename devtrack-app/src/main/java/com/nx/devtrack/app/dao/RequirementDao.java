package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequirementDao extends JpaRepository<Requirement, Long> {

    List<Requirement> findByProjectIdOrderByCreateTimeDesc(Long projectId);
}
