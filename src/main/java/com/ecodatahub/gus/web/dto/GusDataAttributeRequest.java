package com.ecodatahub.gus.web.dto;

import com.ecodatahub.gus.domain.GUSDataAttribute;

public record GusDataAttributeRequest(
        Integer id,
        String name,
        String symbol,
        String description
) {

    public GUSDataAttribute toEntity() {
        GUSDataAttribute attribute = new GUSDataAttribute();
        attribute.setId(id);
        attribute.setName(name);
        attribute.setSymbol(symbol);
        attribute.setDescription(description);
        return attribute;
    }
}
