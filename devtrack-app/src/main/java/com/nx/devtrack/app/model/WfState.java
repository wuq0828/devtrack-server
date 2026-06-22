package com.nx.devtrack.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 工作流状态节点(配置化状态机的状态定义)。看板分列、报表归类用。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "wf_state")
@SQLRestriction("deleted = false")
public class WfState extends BaseModel {

    private Long workflowId;

    private String code;

    private String name;

    /** INITIAL / IN_PROGRESS / DONE / REJECTED,看板分列与报表归类 */
    private String category;

    @Column(name = "is_initial")
    private boolean initial;

    private Integer sortOrder = 0;
}
