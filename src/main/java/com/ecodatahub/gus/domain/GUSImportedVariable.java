package com.ecodatahub.gus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "gus_imported_variables")
public class GUSImportedVariable {

    @Id
    @Column(name = "variable_id", nullable = false)
    private String variableId;

    @Column(name = "subject_id", nullable = false)
    private String subjectId;

    @Column(nullable = false, length = 1000)
    private String name;

    @Column(length = 1000)
    private String subname;

    private Integer level;

    @Column(name = "measure_unit_id")
    private Integer measureUnitId;

    @Column(name = "measure_unit_name")
    private String measureUnitName;
}
