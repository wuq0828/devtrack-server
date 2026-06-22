package com.nx.devtrack.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 评论(dt_comment),多态挂载到任意实体(缺陷/需求/用例)。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "dt_comment")
@SQLRestriction("deleted = false")
public class Comment extends BaseModel {

    private String entityType = "DEFECT";

    private Long entityId;

    @Column(length = 8000)
    private String content;

    private Long authorId;
}
