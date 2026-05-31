package com.ecodatahub.gus.service;

import com.ecodatahub.gus.api.dto.GUSDataRegionDto;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class GusDataAttributeFilter {

    private static final Set<Integer> EXCLUDED_DATA_ATTRIBUTE_IDS = Set.of(
            0, 3, 4, 7, 13, 14, 15, 17, 20, 21, 50, 91
    );

    public GUSDataRegionDto withoutExcludedAttributeValues(GUSDataRegionDto region) {
        if (region.getValues() == null) {
            return region;
        }

        region.setValues(
                region.getValues().stream()
                        .filter(value -> value.getAttrId() == null
                                || !EXCLUDED_DATA_ATTRIBUTE_IDS.contains(value.getAttrId()))
                        .toList()
        );

        return region;
    }
}
