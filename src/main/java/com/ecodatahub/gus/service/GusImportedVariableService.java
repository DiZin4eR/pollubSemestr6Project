package com.ecodatahub.gus.service;

import com.ecodatahub.gus.api.dto.GUSVariableDto;
import com.ecodatahub.gus.domain.GUSImportedVariable;
import com.ecodatahub.gus.repository.GUSImportedVariableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GusImportedVariableService {

    private final GUSImportedVariableRepository repository;

    @Transactional(readOnly = true)
    public List<GUSVariableDto> getBySubjectId(String subjectId) {
        return repository.findBySubjectIdOrderByVariableIdAsc(subjectId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean exists(String variableId) {
        return repository.existsById(variableId);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void save(ImportedVariable importedVariable) {
        GUSImportedVariable variable = repository.findById(importedVariable.variableId())
                .orElseGet(GUSImportedVariable::new);

        variable.setVariableId(importedVariable.variableId());
        variable.setSubjectId(importedVariable.subjectId());
        variable.setName(importedVariable.name());
        variable.setSubname(importedVariable.subname());
        variable.setLevel(importedVariable.level());
        variable.setMeasureUnitId(importedVariable.measureUnitId());
        variable.setMeasureUnitName(importedVariable.measureUnitName());

        repository.save(variable);
    }

    private GUSVariableDto toDto(GUSImportedVariable variable) {
        GUSVariableDto dto = new GUSVariableDto();

        dto.setId(variable.getVariableId());
        dto.setSubjectId(variable.getSubjectId());
        dto.setName(variable.getName());
        dto.setSubname(variable.getSubname());
        dto.setLevel(variable.getLevel());
        dto.setMeasureUnitId(variable.getMeasureUnitId());
        dto.setMeasureUnitName(variable.getMeasureUnitName());

        return dto;
    }

    public record ImportedVariable(
            String variableId,
            String subjectId,
            String name,
            String subname,
            Integer level,
            Integer measureUnitId,
            String measureUnitName
    ) {
    }
}
