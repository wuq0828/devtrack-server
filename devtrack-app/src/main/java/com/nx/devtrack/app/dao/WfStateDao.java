package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.WfState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WfStateDao extends JpaRepository<WfState, Long> {

    List<WfState> findByWorkflowIdOrderBySortOrderAsc(Long workflowId);

    long countByWorkflowId(Long workflowId);
}
