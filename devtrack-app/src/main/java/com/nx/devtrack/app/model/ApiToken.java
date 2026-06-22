package com.nx.devtrack.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 开放 API 机器令牌(intg_api_token)。明文仅在创建时返回一次,库里只存 SHA-256。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "intg_api_token")
@SQLRestriction("deleted = false")
public class ApiToken extends BaseModel {

    private String name;

    @Column(length = 64)
    private String tokenHash;

    /** 机器账号 user id */
    private Long userId;
}
