package com.nx.devtrack.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 标签(dt_tag)。多端/分类标记,如 iOS / Android / 线上 / 性能。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "dt_tag")
@SQLRestriction("deleted = false")
public class Tag extends BaseModel {

    private Long projectId;

    private String name;

    /** 展示颜色,如 #409EFF */
    private String color = "#409EFF";
}
