package com.ecodatahub.gus.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GUSVariableDto {

    private String id;
    private String subjectId;
    @JsonProperty("n1")
    private String name;
    @JsonProperty("n2")
    private String subname;
    private Integer level;
    private Integer measureUnitId;
    private String measureUnitName;

}
