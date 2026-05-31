package com.ecodatahub.gus.web.dto;

import com.ecodatahub.gus.domain.GUSDataAttribute;

public record GusDataAttributeResponse(
        Integer id,
        String name,
        String symbol,
        String description
) {

    public static GusDataAttributeResponse from(GUSDataAttribute attribute) {
        return new GusDataAttributeResponse(
                attribute.getId(),
                attribute.getName(),
                attribute.getSymbol(),
                attribute.getDescription()
        );
    }
}
