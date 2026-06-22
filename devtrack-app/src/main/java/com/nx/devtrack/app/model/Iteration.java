package com.nx.devtrack.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

/**
 * 迭代 / Sprint(dt_iteration)。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "dt_iteration")
@SQLRestriction("deleted = false")
public class Iteration extends BaseModel {

    private Long projectId;

    private String name;

    /** PLANNING / ACTIVE / CLOSED */
    private String status = "ACTIVE";

    private LocalDate startDate;

    private LocalDate endDate;
}
