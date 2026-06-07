package com.ecodatahub.gus.service;

import com.ecodatahub.gus.api.dto.GUSDataRegionDto;
import com.ecodatahub.gus.domain.GUSVariableDataCache;
import com.ecodatahub.gus.repository.GUSVariableDataCacheRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GusVariableDataCacheService {

    private static final Duration MAX_CACHE_AGE = Duration.ofDays(7);
    private static final TypeReference<List<GUSDataRegionDto>> REGION_LIST_TYPE = new TypeReference<>() {
    };

    private final GUSVariableDataCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<CachedVariableData> getFresh(String variableId) {
        return cacheRepository.findById(variableId)
                .filter(this::isFresh)
                .map(this::toCachedVariableData);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public CachedVariableData save(String variableId, List<GUSDataRegionDto> regions) {
        if (variableId == null || variableId.isBlank()) {
            throw new IllegalArgumentException("Variable id is required");
        }

        GUSVariableDataCache cache = cacheRepository.findById(variableId)
                .orElseGet(GUSVariableDataCache::new);
        Instant fetchedAt = Instant.now();

        cache.setVariableId(variableId);
        cache.setDataJson(toJson(regions == null ? List.of() : regions));
        cache.setFetchedAt(fetchedAt);

        cacheRepository.save(cache);

        return new CachedVariableData(regions == null ? List.of() : List.copyOf(regions), fetchedAt);
    }

    private boolean isFresh(GUSVariableDataCache cache) {
        return cache.getFetchedAt() != null
                && cache.getFetchedAt().isAfter(Instant.now().minus(MAX_CACHE_AGE));
    }

    private CachedVariableData toCachedVariableData(GUSVariableDataCache cache) {
        try {
            return new CachedVariableData(
                    objectMapper.readValue(cache.getDataJson(), REGION_LIST_TYPE),
                    cache.getFetchedAt()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read cached GUS variable data", exception);
        }
    }

    private String toJson(List<GUSDataRegionDto> regions) {
        try {
            return objectMapper.writeValueAsString(regions);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not store GUS variable data", exception);
        }
    }

    public record CachedVariableData(
            List<GUSDataRegionDto> regions,
            Instant fetchedAt
    ) {
    }
}
