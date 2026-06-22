package com.nx.devtrack.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 站内信通知(dt_notification)。read 用 read_flag 列名避免数据库保留字。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "dt_notification")
@SQLRestriction("deleted = false")
public class Notification extends BaseModel {

    /** 接收人 */
    private Long userId;

    /** ASSIGN / MENTION */
    private String type;

    @Column(length = 512)
    private String content;

    private String refType;

    private Long refId;

    @Column(name = "read_flag")
    private boolean readFlag;
}
