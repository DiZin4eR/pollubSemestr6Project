package com.ecodatahub.gus.api;

import lombok.extern.slf4j.Slf4j;
import com.ecodatahub.gus.service.GusDataAttributeFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.ecodatahub.gus.api.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
public class GusApiClient {

    private final WebClient webClient;
    private final GusRateLimiter rateLimiter;
    private final GusRateLimitRetryPolicy retryPolicy;
    private final GusDataAttributeFilter dataAttributeFilter;

    public GusApiClient(
            @Qualifier("gusWebClient")
            WebClient webClient,
            GusRateLimiter rateLimiter,
            GusRateLimitRetryPolicy retryPolicy,
            GusDataAttributeFilter dataAttributeFilter) {
        this.webClient = webClient;
        this.rateLimiter = rateLimiter;
        this.retryPolicy = retryPolicy;
        this.dataAttributeFilter = dataAttributeFilter;
    }


    public ResponseEntity<GUSSubjectResponse> getSubjectsPage(
            int page,
            int pageSize,
            String parentId
    ) {
        return retryPolicy.execute(
                () -> {
                    rateLimiter.consume();

                    return webClient.get()
                            .uri(uriBuilder -> {
                                uriBuilder
                                        .path("/subjects")
                                        .queryParam("lang", "pl")
                                        .queryParam("format", "json")
                                        .queryParam("page", page)
                                        .queryParam("page-size", pageSize);

                                if (parentId != null) {
                                    uriBuilder.queryParam(
                                            "parent-id", parentId
                                    );
                                }

                                return uriBuilder.build();
                            })
                            .retrieve()
                            .toEntity(GUSSubjectResponse.class)
                            .block();
                }
        );
    }

    public GUSVariableResponse getVariablePage(
            int page,
            int pageSize,
            String subjectId
    ) {
        return retryPolicy.execute(
                () -> {
                    rateLimiter.consume();

                    return webClient.get()
                            .uri(uriBuilder ->
                                    uriBuilder.path("/variables")
                                    .queryParam("lang", "pl")
                                    .queryParam("format", "json")
                                    .queryParam("page", page)
                                    .queryParam("page-size", pageSize)
                                    .queryParam("subject-id", subjectId)
                                    .build()
                            )
                            .retrieve()
                            .bodyToMono(GUSVariableResponse.class)
                            .block();
                }
        );
    }

    public GUSDataResponse getDataByVariablePage(
            int page,
            int pageSize,
            String variable
    ) {
        return retryPolicy.execute(
                () -> {
                    rateLimiter.consume();

                    return webClient.get()
                            .uri(uriBuilder ->
                                    uriBuilder.path("/data/by-variable")
                                            .pathSegment(variable)
                                            .queryParam("lang", "pl")
                                            .queryParam("format", "json")
                                            .queryParam("page", page)
                                            .queryParam("page-size", pageSize)
                                            .build())
                            .retrieve()
                            .bodyToMono(GUSDataResponse.class)
                            .block();
                }
        );
    }

    public List<GUSDataRegionDto> getDataByVariable(
            String variable
    ) {
        return getDataByVariable(variable, progress -> {
        });
    }

    public List<GUSDataRegionDto> getDataByVariable(
            String variable,
            Consumer<DataFetchProgress> progressConsumer
    ) {
        List<GUSDataRegionDto> allData = new ArrayList<>();
        int currentPage = 0;
        int totalPages = 1;
        GUSDataResponse response;

        do {
            response = getDataByVariablePage(
                    currentPage,
                    100,
                    variable
            );

            if (currentPage == 0) {
                totalPages = getTotalPages(response, 100);
            }

            if (response.getResults() != null) {
                response.getResults().stream()
                        .map(dataAttributeFilter::withoutExcludedAttributeValues)
                        .filter(region -> region.getValues() != null && !region.getValues().isEmpty())
                        .forEach(allData::add);
            }

            currentPage++;
            progressConsumer.accept(new DataFetchProgress(currentPage, totalPages));
        } while (
                response.getLinks() != null &&
                        response.getLinks().hasNextPage()
        );

        return allData;
    }

    private int getTotalPages(GUSDataResponse response, int pageSize) {
        if (response.getTotalRecords() == null || response.getTotalRecords() <= 0) {
            return 1;
        }

        return Math.max(1, (int) Math.ceil((double) response.getTotalRecords() / pageSize));
    }

    public List<GUSSubjectDto> getAllSubjects(
            String parentId
    ) {
        List<GUSSubjectDto> allSubjects = new ArrayList<>();

        int currentPage = 0;

        ResponseEntity<GUSSubjectResponse> response;
        GUSSubjectResponse body;

        do {

            response = getSubjectsPage(
                    currentPage,
                    100,
                    parentId
            );

            body = response.getBody();

            HttpHeaders headers = response.getHeaders();
            String rateLimit = headers.getFirst("X-Rate-Limit-Limit");
            String remainingLimit = headers.getFirst("X-Rate-Limit-Remaining");
            String rateLimitReset = headers.getFirst("X-Rate-Limit-Reset");

            log.info(
                    """
                    GUS API Rate Limits:
                    Limit: {}
                    Remaining: {}
                    Reset: {}
                    """,
                    rateLimit,
                    remainingLimit,
                    rateLimitReset
            );

            if (body.getResults() != null) {

                allSubjects.addAll(
                        body.getResults()
                );
            }

            currentPage++;

        } while (
                body.getLinks() != null
                        &&
                        body.getLinks().hasNextPage()
        );

        return allSubjects;
    }

    public List<GUSVariableDto> getAllVariables(
            String subjectId
    ) {
        List<GUSVariableDto> allVariables = new ArrayList<>();
        int currentPage = 0;
        GUSVariableResponse response;

        do {
            response = getVariablePage(
                    currentPage,
                    100,
                    subjectId
            );

            if (response.getResults() != null) {
                allVariables.addAll(
                        response.getResults()
                );
            }

            currentPage++;
        } while (
                response.getLinks() != null &&
                        response.getLinks().hasNextPage()
        );

        return allVariables;
    }

    public record DataFetchProgress(
            int completedRequests,
            int totalRequests
    ) {
    }
}
