package com.nx.devtrack.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 工作流流转规则(配置化状态机的核心)。
 * 业务代码不写 switch(status),而是读这张表决定「当前态能否经某动作到目标态」。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "wf_transition")
@SQLRestriction("deleted = false")
public class WfTransition extends BaseModel {

    private Long workflowId;

    /** 动作码:confirm/start/resolve/verify/close/reject/reopen */
    private String code;

    /** 按钮文案 */
    private String name;

    /** 起始态;"*" 表示任意态(如 reject) */
    private String fromState;

    private String toState;

    /** 限定角色(如 verify 限 QA),可空 */
    private String requireRole;

    /** 是否强制填写备注(reject/reopen) */
    private boolean requireComment;

    /** 触发的领域事件名(通知/回填),可空 */
    private String hookEvent;
}
