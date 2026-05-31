package com.ecodatahub.gus.web.dto;

import com.ecodatahub.gus.domain.GUSSubject;

import java.util.List;

public record GusSubjectResponse(
        Long id,
        String gusId,
        String parentGusId,
        String name,
        Boolean hasVariables,
        List<String> children,
        List<Integer> levels
) {

    public static GusSubjectResponse from(GUSSubject subject) {
        return new GusSubjectResponse(
                subject.getId(),
                subject.getGusId(),
                subject.getParentGusId(),
                subject.getName(),
                subject.getHasVariables(),
                subject.getChildren(),
                subject.getLevels()
        );
    }
}
