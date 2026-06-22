package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.ApiToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiTokenDao extends JpaRepository<ApiToken, Long> {

    ApiToken findByTokenHash(String tokenHash);

    long count();
}
