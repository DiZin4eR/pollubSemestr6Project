package com.ecodatahub.gus.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(
        name = "gus_subjects",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_gus_subject_gus_id",
                columnNames = "gus_id"
        )
)
public class GUSSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gus_id", nullable = false)
    private String gusId;

    @Column(name = "parent_gus_id")
    private String parentGusId;

    @Column(nullable = false, length = 1000)
    private String name;

    private Boolean hasVariables;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "gus_subject_children",
            joinColumns = @JoinColumn(name = "subject_id")
    )
    @Column(name = "child_gus_id", nullable = false)
    private List<String> children = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "gus_subject_levels",
            joinColumns = @JoinColumn(name = "subject_id")
    )
    @Column(name = "level_value", nullable = false)
    private List<Integer> levels = new ArrayList<>();
}
