package com.ecodatahub.gus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.ecodatahub.gus.domain.GUSSubject;
import com.ecodatahub.gus.domain.GUSSubjectImportState;
import com.ecodatahub.gus.api.dto.GUSSubjectDto;
import com.ecodatahub.gus.repository.GUSSubjectImportStateRepository;
import com.ecodatahub.gus.repository.GUSSubjectRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class GusSubjectImportPersistenceService {

    private static final String ROOT_SUBJECT_PREFIX = "K";

    private final GUSSubjectRepository gusSubjectRepository;
    private final GUSSubjectImportStateRepository importStateRepository;

    public boolean isParentMarkedImported(String parentToken) {
        return importStateRepository.existsByParentKey(parentToken);
    }

    public boolean areStoredChildrenComplete(String parentId, List<GUSSubject> storedSubjects) {
        if (storedSubjects.isEmpty()) {
            return false;
        }

        if (parentId == null) {
            return true;
        }

        return gusSubjectRepository.findByGusId(parentId)
                .map(parent -> allChildrenAreStored(parent.getChildren()))
                .orElse(false);
    }

    public List<GUSSubject> getStoredSubjects(String parentId) {
        if (parentId == null) {
            return gusSubjectRepository.findByParentGusIdIsNullAndGusIdStartingWith(ROOT_SUBJECT_PREFIX);
        }

        return gusSubjectRepository.findByParentGusId(parentId);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public boolean saveSubject(GUSSubjectDto subjectDto) {
        GUSSubject subject = gusSubjectRepository.findByGusId(subjectDto.getId())
                .orElse(null);
        boolean newSubject = subject == null;

        if (newSubject) {
            subject = new GUSSubject();
        }

        subject.setGusId(subjectDto.getId());
        subject.setParentGusId(subjectDto.getParentId());
        subject.setName(subjectDto.getName());
        subject.setHasVariables(subjectDto.getHasVariables());
        subject.setChildren(emptyIfNull(subjectDto.getChildren()));
        subject.setLevels(emptyIfNull(subjectDto.getLevels()));

        gusSubjectRepository.save(subject);

        return newSubject;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void markParentImported(String parentToken) {
        GUSSubjectImportState state = importStateRepository.findByParentKey(parentToken)
                .orElseGet(GUSSubjectImportState::new);

        state.setParentKey(parentToken);
        state.setImportedAt(Instant.now());

        importStateRepository.save(state);
    }

    private boolean allChildrenAreStored(List<String> children) {
        if (children == null || children.isEmpty()) {
            return true;
        }

        Set<String> childIds = Set.copyOf(children);

        return gusSubjectRepository.countByGusIdIn(childIds) == childIds.size();
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }
}
