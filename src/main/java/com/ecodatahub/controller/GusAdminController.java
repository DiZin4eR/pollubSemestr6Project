package com.ecodatahub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ecodatahub.gus.api.GusApiClient;
import com.ecodatahub.gus.api.dto.GUSDataRegionDto;
import com.ecodatahub.gus.api.dto.GUSSubjectDto;
import com.ecodatahub.gus.api.dto.GUSVariableDto;
import com.ecodatahub.gus.service.GusSubjectImportService;
import com.ecodatahub.gus.service.GusVariableDataCacheService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/gus")
@RequiredArgsConstructor
public class GusAdminController {

    private final GusApiClient gusClient;
    private final GusSubjectImportService gusSubjectImportService;
    private final GusVariableDataCacheService variableDataCacheService;

    @GetMapping("/subjects")
    public List<GUSSubjectDto> getRootSubjects() {
        return gusClient.getAllSubjects(null);
    }

    @GetMapping("/subjects/{parentId}")
    public List<GUSSubjectDto> getSubjects(@PathVariable String parentId) {
        return gusClient.getAllSubjects(parentId);
    }

    @GetMapping("/variables/{subjectId}")
    public List<GUSVariableDto> getVariables(@PathVariable String subjectId) {
        return gusClient.getAllVariables(subjectId);
    }

    @GetMapping("/data/by-variable/{variable}")
    public List<GUSDataRegionDto> getDataByVariable(@PathVariable String variable) {
        return variableDataCacheService.getFresh(variable)
                .orElseGet(() -> variableDataCacheService.save(variable, gusClient.getDataByVariable(variable)))
                .regions();
    }

    @GetMapping("/subjects/import")
    public int importSubjects() {
        return gusSubjectImportService.importAllSubjects();
    }

}
