package com.nx.devtrack.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 附件(dt_attachment)。多态挂载;DB 只存 object_key,实际二进制在 StorageService。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "dt_attachment")
@SQLRestriction("deleted = false")
public class Attachment extends BaseModel {

    private String entityType = "DEFECT";

    private Long entityId;

    private String fileName;

    @Column(length = 512)
    private String objectKey;

    private Long sizeBytes;

    private Long uploaderId;
}
