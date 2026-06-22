package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TagDao extends JpaRepository<Tag, Long> {

    List<Tag> findByProjectIdOrderByCreateTimeAsc(Long projectId);

    List<Tag> findByIdIn(List<Long> ids);
}
