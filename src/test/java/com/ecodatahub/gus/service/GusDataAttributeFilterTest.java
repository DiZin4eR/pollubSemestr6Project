package com.ecodatahub.gus.service;

import com.ecodatahub.gus.api.dto.GUSDataDto;
import com.ecodatahub.gus.api.dto.GUSDataRegionDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GusDataAttributeFilterTest {

    private final GusDataAttributeFilter filter = new GusDataAttributeFilter();

    @Test
    void withoutExcludedAttributeValuesKeepsNullAndAllowedAttributes() {
        GUSDataRegionDto region = new GUSDataRegionDto();
        GUSDataDto nullAttribute = value(null);
        GUSDataDto allowedAttribute = value(42);
        GUSDataDto excludedAttribute = value(3);
        region.setValues(List.of(nullAttribute, allowedAttribute, excludedAttribute));

        GUSDataRegionDto result = filter.withoutExcludedAttributeValues(region);

        assertThat(result).isSameAs(region);
        assertThat(result.getValues()).containsExactly(nullAttribute, allowedAttribute);
    }

    @Test
    void withoutExcludedAttributeValuesLeavesNullValuesListUntouched() {
        GUSDataRegionDto region = new GUSDataRegionDto();

        GUSDataRegionDto result = filter.withoutExcludedAttributeValues(region);

        assertThat(result).isSameAs(region);
        assertThat(result.getValues()).isNull();
    }

    private GUSDataDto value(Integer attrId) {
        GUSDataDto dto = new GUSDataDto();
        dto.setAttrId(attrId);
        return dto;
    }
}
