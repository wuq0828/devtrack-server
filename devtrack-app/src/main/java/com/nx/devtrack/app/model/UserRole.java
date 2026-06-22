package com.nx.devtrack.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 用户-角色绑定(带数据范围)。
 * projectId 为 NULL 表示全局角色(如 SYS_ADMIN);否则该角色仅在指定项目生效。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "sys_user_role")
@SQLRestriction("deleted = false")
public class UserRole extends BaseModel {

    private Long userId;

    private Long roleId;

    /** 角色生效的项目;全局角色为 null */
    private Long projectId;
}
