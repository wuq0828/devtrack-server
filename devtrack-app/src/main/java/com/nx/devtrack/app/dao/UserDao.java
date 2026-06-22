package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDao extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    User findByUsername(String username);

    User findByUsernameAndPassword(String username, String password);

    User findByLoginToken(String loginToken);

    User findByFeishuOpenId(String feishuOpenId);
}
