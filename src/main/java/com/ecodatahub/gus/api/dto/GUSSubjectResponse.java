package com.ecodatahub.gus.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class GUSSubjectResponse {

    private Integer totalRecords;
    private Integer page;
    private Integer pageSize;
    private GUSLinksDto links;
    private List<GUSSubjectDto> results;

}
