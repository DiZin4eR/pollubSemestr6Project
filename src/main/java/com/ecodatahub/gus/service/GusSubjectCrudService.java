package com.ecodatahub.gus.service;

import com.ecodatahub.gus.domain.GUSSubject;
import com.ecodatahub.gus.repository.GUSSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class GusSubjectCrudService {

    private final GUSSubjectRepository gusSubjectRepository;

    public List<GUSSubject> getGusSubjects() {
        return gusSubjectRepository.findAll();
    }

    public Optional<GUSSubject> getGusSubject(Long id) {
        return gusSubjectRepository.findById(id);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public GUSSubject createGusSubject(GUSSubject subject) {
        subject.setId(null);

        return gusSubjectRepository.save(subject);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Optional<GUSSubject> updateGusSubject(Long id, GUSSubject subject) {
        if (gusSubjectRepository.findById(id).isEmpty()) {
            return Optional.empty();
        }

        subject.setId(id);

        return Optional.of(gusSubjectRepository.save(subject));
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public boolean deleteGusSubject(Long id) {
        return gusSubjectRepository.findById(id)
                .map(subject -> {
                    gusSubjectRepository.delete(subject);
                    return true;
                })
                .orElse(false);
    }
}
