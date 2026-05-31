package com.ecodatahub.gus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ecodatahub.gus.domain.GUSSubject;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GUSSubjectRepository extends JpaRepository<GUSSubject, Long> {

    Optional<GUSSubject> findByGusId(String gusId);

    List<GUSSubject> findByParentGusId(String parentGusId);

    List<GUSSubject> findByParentGusIdIsNull();

    List<GUSSubject> findByParentGusIdIsNullAndGusIdStartingWith(String prefix);

    long countByGusIdIn(Collection<String> gusIds);

    long countByHasVariablesTrue();
}
