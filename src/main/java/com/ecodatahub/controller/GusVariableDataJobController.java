package com.ecodatahub.controller;

import com.ecodatahub.gus.service.GusVariableDataJobService;
import com.ecodatahub.gus.web.dto.GusVariableDataJobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gus/variable-data-jobs")
@RequiredArgsConstructor
public class GusVariableDataJobController {

    private final GusVariableDataJobService jobService;

    @PostMapping
    public GusVariableDataJobResponse start(
            @RequestParam String variableId,
            @RequestParam(required = false) String subjectId
    ) {
        return GusVariableDataJobResponse.from(jobService.start(variableId, subjectId));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<GusVariableDataJobResponse> get(@PathVariable String jobId) {
        return jobService.get(jobId)
                .map(GusVariableDataJobResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
