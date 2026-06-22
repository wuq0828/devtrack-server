package com.nx.devtrack.app.dao;

import com.nx.devtrack.app.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationDao extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<Notification> findByUserIdAndReadFlagFalseOrderByCreateTimeDesc(Long userId);

    long countByUserIdAndReadFlagFalse(Long userId);
}
