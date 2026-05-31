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
    private final Map<String, VariableDataJob> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public VariableDataJob start(String variableId) {
        String jobId = UUID.randomUUID().toString();
        VariableDataJob job = new VariableDataJob(jobId, variableId);

        jobs.put(jobId, job);

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
            List<GUSDataRegionDto> regions = gusApiClient.getDataByVariable(
                    job.getVariableId(),
                    progress -> job.updateProgress(progress.completedRequests(), progress.totalRequests())
            );

            job.markCompleted(regions);
        } catch (RuntimeException exception) {
            job.markFailed(exception.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }

    public static class VariableDataJob {

        private final String id;
        private final String variableId;
        private final Instant createdAt = Instant.now();
        private volatile String state = "queued";
        private volatile int completedRequests;
        private volatile int totalRequests = 1;
        private volatile String message = "Queued";
        private volatile List<GUSDataRegionDto> regions = List.of();

        private VariableDataJob(String id, String variableId) {
            this.id = id;
            this.variableId = variableId;
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
    }
}
