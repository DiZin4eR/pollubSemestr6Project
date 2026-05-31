package com.ecodatahub.gus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.ecodatahub.gus.domain.GUSSubject;
import com.ecodatahub.gus.api.GusApiClient;
import com.ecodatahub.gus.api.dto.GUSSubjectDto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GusSubjectImportService {

    private static final String ROOT_PARENT = "__root__";

    private final GusApiClient gusClient;
    private final GusSubjectImportPersistenceService persistenceService;

    public int importAllSubjects() {
        int importedCount = 0;
        Queue<String> parentsToRead = new ArrayDeque<>();
        Set<String> visitedParents = new HashSet<>();

        parentsToRead.add(ROOT_PARENT);

        while (!parentsToRead.isEmpty()) {
            String parentToken = parentsToRead.poll();
            String parentId = ROOT_PARENT.equals(parentToken) ? null : parentToken;

            if (!visitedParents.add(parentToken)) {
                continue;
            }

            List<GUSSubject> storedSubjects = persistenceService.getStoredSubjects(parentId);
            List<GUSSubjectDto> subjects;

            if (isParentAlreadyImported(parentToken, parentId, storedSubjects)) {
                subjects = storedSubjects.stream()
                        .map(this::toDto)
                        .toList();

                log.info(
                        "Skipped GUS API for parent {}. Using {} stored subjects.",
                        parentId,
                        subjects.size()
                );
            } else {
                subjects = gusClient.getAllSubjects(parentId);
                log.info("Fetched {} GUS subjects for parent {}", subjects.size(), parentId);

                for (GUSSubjectDto subjectDto : subjects) {
                    if (persistenceService.saveSubject(subjectDto)) {
                        importedCount++;
                    }
                }

                persistenceService.markParentImported(parentToken);
            }

            for (GUSSubjectDto subjectDto : subjects) {
                if (hasChildSubjects(subjectDto)) {
                    parentsToRead.add(subjectDto.getId());
                }
            }
        }

        return importedCount;
    }

    private boolean isParentAlreadyImported(
            String parentToken,
            String parentId,
            List<GUSSubject> storedSubjects
    ) {
        if (persistenceService.isParentMarkedImported(parentToken)) {
            return true;
        }

        boolean imported = persistenceService.areStoredChildrenComplete(parentId, storedSubjects);

        if (imported) {
            persistenceService.markParentImported(parentToken);
        }

        return imported;
    }

    private boolean hasChildSubjects(GUSSubjectDto subjectDto) {
        return subjectDto.getId() != null
                && subjectDto.getChildren() != null
                && !subjectDto.getChildren().isEmpty();
    }

    private GUSSubjectDto toDto(GUSSubject subject) {
        GUSSubjectDto dto = new GUSSubjectDto();

        dto.setId(subject.getGusId());
        dto.setParentId(subject.getParentGusId());
        dto.setName(subject.getName());
        dto.setHasVariables(subject.getHasVariables());
        dto.setChildren(emptyIfNull(subject.getChildren()));
        dto.setLevels(emptyIfNull(subject.getLevels()));

        return dto;
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
