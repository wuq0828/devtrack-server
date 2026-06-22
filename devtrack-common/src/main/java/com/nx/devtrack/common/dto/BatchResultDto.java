package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BatchResultDto implements Serializable {

    private Long defectId;
    private boolean success;
    private String message;

    public static BatchResultDto of(Long defectId, boolean success, String message) {
        BatchResultDto d = new BatchResultDto();
        d.setDefectId(defectId);
        d.setSuccess(success);
        d.setMessage(message);
        return d;
    }
}
