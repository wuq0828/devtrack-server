package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentDao extends JpaRepository<Attachment, Long> {

    List<Attachment> findByEntityTypeAndEntityIdOrderByCreateTimeDesc(String entityType, Long entityId);
}
