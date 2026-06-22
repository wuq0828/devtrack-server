package com.nx.devtrack.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 审批(dt_approval)。缺陷关闭/拒绝等动作走飞书审批;dev 模式由人工 decide。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "dt_approval")
@SQLRestriction("deleted = false")
public class Approval extends BaseModel {

    private Long projectId;

    private Long defectId;

    /** CLOSE / REJECT */
    private String action;

    /** PENDING / APPROVED / REJECTED */
    private String status = "PENDING";

    private Long applicantId;

    /** 飞书审批实例 code(真实接入时回填) */
    private String feishuInstanceCode;
}
