package com.ecodatahub.gus.web.dto;

import com.ecodatahub.gus.domain.GUSSubject;

import java.util.ArrayList;
import java.util.List;

public record GusSubjectRequest(
        String gusId,
        String parentGusId,
        String name,
        Boolean hasVariables,
        List<String> children,
        List<Integer> levels
) {

    public GUSSubject toEntity() {
        GUSSubject subject = new GUSSubject();
        subject.setGusId(gusId);
        subject.setParentGusId(parentGusId);
        subject.setName(name);
        subject.setHasVariables(hasVariables);
        subject.setChildren(children == null ? new ArrayList<>() : new ArrayList<>(children));
        subject.setLevels(levels == null ? new ArrayList<>() : new ArrayList<>(levels));
        return subject;
    }
}
