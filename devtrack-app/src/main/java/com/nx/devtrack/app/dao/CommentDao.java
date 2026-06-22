package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentDao extends JpaRepository<Comment, Long> {

    List<Comment> findByEntityTypeAndEntityIdOrderByCreateTimeAsc(String entityType, Long entityId);
}
