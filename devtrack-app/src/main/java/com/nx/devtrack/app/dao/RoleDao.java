package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleDao extends JpaRepository<Role, Long> {

    Role findByCode(String code);

    List<Role> findByIdIn(List<Long> ids);
}
