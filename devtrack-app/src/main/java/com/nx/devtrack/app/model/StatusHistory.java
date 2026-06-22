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
 * 状态流转不可变流水(dt_status_history)。每次成功流转写一条,不更新不软删。
 * duration_sec 记录在前一状态停留秒数,供"平均处理时长"等报表用。
 */
@Data
@Entity
@Table(name = "dt_status_history")
public class StatusHistory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType = "DEFECT";

    private Long entityId;

    private String fromState;

    private String toState;

    private String transitionCode;

    private Long operatorId;

    private Long durationSec;

    @Column(length = 1024)
    private String comment;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
}
