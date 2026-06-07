package com.ecodatahub.gus.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "gus_variable_data_cache")
public class GUSVariableDataCache {

    @Id
    @Column(name = "variable_id", nullable = false)
    private String variableId;

    @Lob
    @Column(name = "data_json", nullable = false, columnDefinition = "LONGTEXT")
    private String dataJson;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;
}
