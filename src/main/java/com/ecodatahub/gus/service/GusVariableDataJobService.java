package com.ecodatahub.gus.service;

import com.ecodatahub.gus.api.GusApiClient;
import com.ecodatahub.gus.api.dto.GUSDataRegionDto;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class GusVariableDataJobService {

    private final GusApiClient gusApiClient;
    private final GusVariableDataCacheService cacheService;
    private final GusImportedVariableService importedVariableService;
    private final Map<String, VariableDataJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public VariableDataJob start(String variableId) {
        return start(variableId, null);
    }

    public VariableDataJob start(String variableId, String subjectId) {
        String jobId = UUID.randomUUID().toString();
        VariableDataJob job = new VariableDataJob(jobId, variableId, subjectId);

        jobs.put(jobId, job);

        cacheService.getFresh(variableId)
                .ifPresent(cachedData -> job.markCompletedFromDatabase(cachedData.regions(), cachedData.fetchedAt()));

        if ("completed".equals(job.getState())) {
            return job;
        }

        executorService.submit(() -> run(job));

        return job;
    }

    public Optional<VariableDataJob> get(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(jobs.get(jobId));
    }

    private void run(VariableDataJob job) {
        job.markRunning();

        try {
            validateVariableExists(job);

            List<GUSDataRegionDto> regions = gusApiClient.getDataByVariable(
                    job.getVariableId(),
                    progress -> job.updateProgress(progress.completedRequests(), progress.totalRequests())
            );
            GusVariableDataCacheService.CachedVariableData cachedData = cacheService.save(job.getVariableId(), regions);

            job.markCompletedFromApi(cachedData.regions(), cachedData.fetchedAt());
        } catch (RuntimeException exception) {
            job.markFailed(exception.getMessage());
        }
    }

    private void validateVariableExists(VariableDataJob job) {
        if (job.getSubjectId() == null || job.getSubjectId().isBlank()) {
            return;
        }

        if (importedVariableService.exists(job.getVariableId())) {
            return;
        }

        boolean exists = gusApiClient.getAllVariables(job.getSubjectId()).stream()
                .anyMatch(variable -> job.getVariableId().equals(variable.getId()));

        if (!exists) {
            throw new IllegalArgumentException(
                    "Variable " + job.getVariableId() + " does not exist in GUS subject " + job.getSubjectId()
            );
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }

    public static class VariableDataJob {

        private final String id;
        private final String variableId;
        private final String subjectId;
        private final Instant createdAt = Instant.now();
        private volatile String state = "queued";
        private volatile int completedRequests;
        private volatile int totalRequests = 1;
        private volatile String message = "Queued";
        private volatile List<GUSDataRegionDto> regions = List.of();
        private volatile Instant dataFetchedAt;
        private volatile String dataSource = "api";

        private VariableDataJob(String id, String variableId, String subjectId) {
            this.id = id;
            this.variableId = variableId;
            this.subjectId = subjectId;
        }

        public void markRunning() {
            state = "running";
            message = "Fetching GUS data";
        }

        public void updateProgress(int completedRequests, int totalRequests) {
            this.completedRequests = Math.max(0, completedRequests);
            this.totalRequests = Math.max(1, totalRequests);
            message = "Fetched " + this.completedRequests + " of " + this.totalRequests + " GUS pages";
        }

        public void markCompleted(List<GUSDataRegionDto> regions) {
            this.regions = regions == null ? List.of() : List.copyOf(regions);
            this.completedRequests = Math.max(completedRequests, totalRequests);
            state = "completed";
            message = "Completed";
        }

        public void markCompletedFromDatabase(List<GUSDataRegionDto> regions, Instant fetchedAt) {
            markCompleted(regions);
            this.dataFetchedAt = fetchedAt;
            this.dataSource = "database";
            message = "Loaded from database";
        }

        public void markCompletedFromApi(List<GUSDataRegionDto> regions, Instant fetchedAt) {
            markCompleted(regions);
            this.dataFetchedAt = fetchedAt;
            this.dataSource = "api";
            message = "Fetched from GUS API and saved to database";
        }

        public void markFailed(String message) {
            state = "failed";
            this.message = message == null || message.isBlank() ? "Request failed" : message;
        }

        public String getId() {
            return id;
        }

        public String getVariableId() {
            return variableId;
        }

        public String getSubjectId() {
            return subjectId;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public String getState() {
            return state;
        }

        public int getCompletedRequests() {
            return completedRequests;
        }

        public int getTotalRequests() {
            return totalRequests;
        }

        public int getProgressPercent() {
            if (totalRequests <= 0) {
                return 0;
            }

            return Math.min(100, Math.round((completedRequests * 100f) / totalRequests));
        }

        public String getMessage() {
            return message;
        }

        public List<GUSDataRegionDto> getRegions() {
            return regions;
        }

        public Instant getDataFetchedAt() {
            return dataFetchedAt;
        }

        public String getDataSource() {
            return dataSource;
        }
    }
}
