package com.ecodatahub.gus.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class GUSDataResponse {

    private Long totalRecords;
    private GUSLinksDto links;
    private Integer variableId;
    private Integer measureUnitId;
    private Integer aggregateId;
    private Object lastUpdate;
    private List<GUSDataRegionDto> results;

}
