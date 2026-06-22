package com.nx.devtrack.app.model;

import com.nx.devtrack.common.enums.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 需求(dt_requirement)。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "dt_requirement")
@SQLRestriction("deleted = false")
public class Requirement extends BaseModel {

    private Long projectId;

    private String title;

    @Column(length = 8000)
    private String description;

    /** PENDING / IN_PROGRESS / DONE */
    private String status = "PENDING";

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.P2;

    private Long iterationId;

    private Long reporterId;
}
