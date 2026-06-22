package com.nx.devtrack.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 缺陷自定义字段定义(cf_field_def)。值存在 dt_defect.custom_fields(JSON 列),定义/校验查本表。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "cf_field_def")
@SQLRestriction("deleted = false")
public class CustomFieldDef extends BaseModel {

    private Long projectId;

    /** 字段键,对应 custom_fields JSON 的 key */
    private String fieldKey;

    private String label;

    /** TEXT / NUMBER / SELECT */
    private String fieldType = "TEXT";

    /** SELECT 选项,JSON 数组字符串 */
    @Column(length = 1024)
    private String options;

    private boolean required;
}
