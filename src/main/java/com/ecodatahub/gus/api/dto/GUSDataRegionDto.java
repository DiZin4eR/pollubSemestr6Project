package com.ecodatahub.gus.api.dto;

import lombok.Data;

import java.util.List;

@Data
public class GUSDataRegionDto {

    private String id;
    private String name;
    private List<GUSDataDto> values;

}
