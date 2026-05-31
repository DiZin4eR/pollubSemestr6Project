package com.ecodatahub.gus.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class GUSSubjectDto {

    private String id;
    private String parentId;
    private String name;
    private Boolean hasVariables;
    private List<String> children;
    private List<Integer> levels;

}
