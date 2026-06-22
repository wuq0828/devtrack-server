package com.nx.devtrack.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 角色。scope 区分全局角色(SYS_ADMIN)与项目角色(PROJECT_OWNER/DEV/QA/PM/GUEST)。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "sys_role")
@SQLRestriction("deleted = false")
public class Role extends BaseModel {

    private String code;

    private String name;

    /** GLOBAL / PROJECT */
    private String scope = "PROJECT";
}
