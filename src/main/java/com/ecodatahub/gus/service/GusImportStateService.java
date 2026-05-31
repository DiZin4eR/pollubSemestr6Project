package com.ecodatahub.gus.service;

import com.ecodatahub.gus.domain.GUSSubjectImportState;
import com.ecodatahub.gus.repository.GUSSubjectImportStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class GusImportStateService {

    private final GUSSubjectImportStateRepository gusSubjectImportStateRepository;

    public List<GUSSubjectImportState> getGusSubjectImportStates() {
        return gusSubjectImportStateRepository.findAll();
    }

    public Optional<GUSSubjectImportState> getGusSubjectImportState(Long id) {
        return gusSubjectImportStateRepository.findById(id);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public GUSSubjectImportState createGusSubjectImportState(GUSSubjectImportState state) {
        state.setId(null);

        return gusSubjectImportStateRepository.save(state);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Optional<GUSSubjectImportState> updateGusSubjectImportState(Long id, GUSSubjectImportState state) {
        if (gusSubjectImportStateRepository.findById(id).isEmpty()) {
            return Optional.empty();
        }

        state.setId(id);

        return Optional.of(gusSubjectImportStateRepository.save(state));
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public boolean deleteGusSubjectImportState(Long id) {
        return gusSubjectImportStateRepository.findById(id)
                .map(state -> {
                    gusSubjectImportStateRepository.delete(state);
                    return true;
                })
                .orElse(false);
    }
}
