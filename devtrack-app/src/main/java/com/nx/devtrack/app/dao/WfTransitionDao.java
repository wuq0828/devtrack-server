package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.WfTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WfTransitionDao extends JpaRepository<WfTransition, Long> {

    /**
     * 匹配某工作流下「从 fromState 经动作 code」的流转规则。
     * 精确匹配传具体状态;通配匹配(如 reject 从任意态)传 fromState = "*"。
     */
    WfTransition findByWorkflowIdAndFromStateAndCode(Long workflowId, String fromState, String code);

    List<WfTransition> findByWorkflowId(Long workflowId);

    long countByWorkflowId(Long workflowId);
}
