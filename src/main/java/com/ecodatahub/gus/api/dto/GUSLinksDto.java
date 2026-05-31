package com.ecodatahub.gus.api.dto;

import lombok.Data;

@Data
public class GUSLinksDto {

    private String first;
    private String prev;
    private String self;
    private String next;
    private String last;

    public boolean hasNextPage() {
        return next != null;
    }

}
