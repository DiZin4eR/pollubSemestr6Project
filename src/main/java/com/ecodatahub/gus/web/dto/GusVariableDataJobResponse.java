package com.ecodatahub.gus.web.dto;

import com.ecodatahub.gus.service.GusVariableDataJobService.VariableDataJob;

public record GusVariableDataJobResponse(
        String id,
        String variableId,
        String state,
        int completedRequests,
        int totalRequests,
        int progressPercent,
        String message
) {

    public static GusVariableDataJobResponse from(VariableDataJob job) {
        return new GusVariableDataJobResponse(
                job.getId(),
                job.getVariableId(),
                job.getState(),
                job.getCompletedRequests(),
                job.getTotalRequests(),
                job.getProgressPercent(),
                job.getMessage()
        );
    }
}
