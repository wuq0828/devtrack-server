package com.nx.devtrack.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 所有实体基类,对齐 nx-skyline 的 BaseModel(来自 nx-common-springboot-jpa):
 * 自带主键 + 审计字段(createTime/updateTime)+ 软删标志 deleted。
 *
 * 字段名驼峰,Hibernate 默认 CamelCaseToUnderscores 命名策略自动转蛇形列:
 * createTime -> create_time, updateTime -> update_time。
 *
 * 生产可替换为 nx-common-springboot-jpa 的 BaseModel,删除本类。
 */
@Data
@MappedSuperclass
public abstract class BaseModel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    /** 软删标志,配合各实体上的 @SQLRestriction("deleted = false") 自动过滤 */
    @Column(nullable = false)
    private boolean deleted = false;
}
