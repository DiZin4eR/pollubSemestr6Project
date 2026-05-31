package com.ecodatahub.gus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ecodatahub.gus.domain.GUSSubjectImportState;

import java.util.Optional;

public interface GUSSubjectImportStateRepository extends JpaRepository<GUSSubjectImportState, Long> {

    boolean existsByParentKey(String parentKey);

    Optional<GUSSubjectImportState> findByParentKey(String parentKey);
}
