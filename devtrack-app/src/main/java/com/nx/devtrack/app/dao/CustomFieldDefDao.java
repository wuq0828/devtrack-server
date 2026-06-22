package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.CustomFieldDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomFieldDefDao extends JpaRepository<CustomFieldDef, Long> {

    List<CustomFieldDef> findByProjectIdOrderByCreateTimeAsc(Long projectId);
}
