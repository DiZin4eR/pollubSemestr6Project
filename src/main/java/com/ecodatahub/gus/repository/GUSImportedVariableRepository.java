package com.ecodatahub.gus.repository;

import com.ecodatahub.gus.domain.GUSImportedVariable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GUSImportedVariableRepository extends JpaRepository<GUSImportedVariable, String> {

    List<GUSImportedVariable> findBySubjectIdOrderByVariableIdAsc(String subjectId);
}
