package com.nx.devtrack.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 标签挂载关系(dt_entity_tag)。多态:DEFECT/REQUIREMENT/TEST_CASE。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "dt_entity_tag")
@SQLRestriction("deleted = false")
public class EntityTag extends BaseModel {

    private String entityType = "DEFECT";

    private Long entityId;

    private Long tagId;
}
