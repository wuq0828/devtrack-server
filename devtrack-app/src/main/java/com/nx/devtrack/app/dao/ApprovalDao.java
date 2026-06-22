package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalDao extends JpaRepository<Approval, Long> {

    List<Approval> findByProjectIdOrderByCreateTimeDesc(Long projectId);

    List<Approval> findByDefectIdOrderByCreateTimeDesc(Long defectId);
}
