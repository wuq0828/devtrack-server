package com.nx.devtrack.app.model;

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
 * 需求-缺陷-用例三向关联(dt_relation)。多态:source/target 各带类型 + id。
 * relationType: COVERS(用例覆盖需求) / VERIFIES(用例验证缺陷) / CAUSED_BY(缺陷源于需求) / RELATES / BLOCKS / DUPLICATES
 */
@Data
@Entity
@Table(name = "dt_relation")
public class Relation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sourceType;
    private Long sourceId;
    private String targetType;
    private Long targetId;
    private String relationType;

    @CreationTimestamp
    private LocalDateTime createTime;
}
