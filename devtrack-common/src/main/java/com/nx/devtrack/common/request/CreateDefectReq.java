package com.nx.devtrack.common.request;

import com.nx.devtrack.common.enums.Priority;
import com.nx.devtrack.common.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateDefectReq extends BaseInnerRequest {

    @NotNull
    private Long projectId;

    @NotBlank
    private String title;

    private String description;

    private Severity severity = Severity.MAJOR;

    private Priority priority = Priority.P2;

    /** 指派人用户 id,可空 */
    private Long assigneeId;

    /** 所属迭代 id,可空 */
    private Long iterationId;
}
