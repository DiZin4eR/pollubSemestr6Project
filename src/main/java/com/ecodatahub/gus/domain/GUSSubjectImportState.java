package com.ecodatahub.gus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(
        name = "gus_subject_import_states",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_gus_subject_import_state_parent",
                columnNames = "parent_key"
        )
)
public class GUSSubjectImportState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_key", nullable = false)
    private String parentKey;

    @Column(nullable = false)
    private Instant importedAt;
}
