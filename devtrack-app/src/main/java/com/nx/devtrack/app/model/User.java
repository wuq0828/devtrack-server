package com.nx.devtrack.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 用户实体,对齐 nx-skyline User:登录 token 存库(loginToken)+ lastLoginTime 判过期。
 * 生产建议 token 同步写 Redis 缓存(login_token_2_user:{token})减少 DB 压力。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "dt_user")
@SQLRestriction("deleted = false")
public class User extends BaseModel {

    private String username;

    /** 演示用明文;生产必须改为 BCrypt 哈希 */
    private String password;

    private String displayName;

    /** 飞书 open_id,用于消息通知 @ 与卡片推送 */
    private String feishuOpenId;

    private String loginToken;

    private LocalDateTime lastLoginTime;

    private boolean admin;
}
