package com.ecodatahub.gus.web.dto;

import com.ecodatahub.gus.domain.GUSSubjectImportState;

import java.time.Instant;

public record GusImportStateResponse(
        Long id,
        String parentKey,
        Instant importedAt
) {

    public static GusImportStateResponse from(GUSSubjectImportState state) {
        return new GusImportStateResponse(
                state.getId(),
                state.getParentKey(),
                state.getImportedAt()
        );
    }
}
