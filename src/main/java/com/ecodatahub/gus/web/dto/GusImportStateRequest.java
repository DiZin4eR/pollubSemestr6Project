package com.ecodatahub.gus.web.dto;

import com.ecodatahub.gus.domain.GUSSubjectImportState;

import java.time.Instant;

public record GusImportStateRequest(
        String parentKey,
        Instant importedAt
) {

    public GUSSubjectImportState toEntity() {
        GUSSubjectImportState state = new GUSSubjectImportState();
        state.setParentKey(parentKey);
        state.setImportedAt(importedAt);
        return state;
    }
}
