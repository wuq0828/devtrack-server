package com.nx.devtrack.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作历史/审计(dt_activity_log)。不可变,记录谁在什么实体上做了什么。
 */
@Data
@Entity
@Table(name = "dt_activity_log")
public class ActivityLog implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType = "DEFECT";

    private Long entityId;

    private String action;

    @Column(length = 2048)
    private String detail;

    private Long operatorId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
}
