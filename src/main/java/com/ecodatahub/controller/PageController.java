package com.ecodatahub.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.ecodatahub.gus.domain.GUSDataAttribute;
import com.ecodatahub.gus.domain.GUSSubject;
import com.ecodatahub.gus.api.dto.GUSSubjectDto;
import com.ecodatahub.news.domain.NewsArticle;
import com.ecodatahub.gus.api.dto.GUSDataRegionDto;
import com.ecodatahub.gus.api.dto.GUSVariableDto;
import com.ecodatahub.gus.service.GusSubjectImportPersistenceService;
import com.ecodatahub.gus.service.GusSubjectTreeService;
import com.ecodatahub.gus.service.GusSubjectTreeService.SubjectNode;
import com.ecodatahub.gus.service.GusSubjectTreeService.SubjectTreeResult;
import com.ecodatahub.gus.service.GusDataAttributeService;
import com.ecodatahub.gus.service.GusImportedVariableService;
import com.ecodatahub.gus.service.GusVariableDataCacheService;
import com.ecodatahub.gus.service.GusVariableDataJobService;
import com.ecodatahub.gus.service.GusVariableDataJobService.VariableDataJob;
import com.ecodatahub.gus.repository.GUSSubjectRepository;
import com.ecodatahub.gus.api.GusApiClient;
import com.ecodatahub.news.service.NewsArticleService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final GUSSubjectRepository gusSubjectRepository;
    private final GusApiClient gusClient;
    private final GusDataAttributeService gusDataAttributeService;
    private final NewsArticleService newsArticleService;
    private final GusSubjectTreeService gusSubjectTreeService;
    private final GusVariableDataJobService gusVariableDataJobService;
    private final GusSubjectImportPersistenceService gusSubjectImportPersistenceService;
    private final GusVariableDataCacheService gusVariableDataCacheService;
    private final GusImportedVariableService gusImportedVariableService;
    private final ObjectMapper objectMapper;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/indicators")
    public String indicators(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "id") String sort,
            Model model
    ) {
        SubjectTreeResult result = gusSubjectTreeService.getSubjectTree(q, type, sort);

        model.addAttribute("subjectTree", result.subjectTree());
        model.addAttribute("subjectsWithVariablesCount", result.subjectsWithVariablesCount());
        model.addAttribute("visibleSubjectsCount", result.visibleSubjectsCount());
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("sort", sort);

        return "indicators";
    }

    @GetMapping(value = "/indicators/export.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public IndicatorsExport indicatorsJson(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "id") String sort
    ) {
        SubjectTreeResult result = gusSubjectTreeService.getSubjectTree(q, type, sort);

        return new IndicatorsExport(
                q,
                type,
                sort,
                result.subjectsWithVariablesCount(),
                result.visibleSubjectsCount(),
                result.subjectTree()
        );
    }

    @PostMapping(value = "/indicators/import.json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String importIndicatorsJson(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes
    ) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("importError", "Select a JSON file to import.");
            return "redirect:/indicators/import";
        }

        try {
            JsonNode root = objectMapper.readTree(file.getInputStream());
            List<GUSSubjectDto> subjects = readImportedSubjects(root);
            List<ImportedVariableData> variableData = readImportedVariableData(root);
            prepareFullImport(subjects, variableData);
            GusSubjectImportPersistenceService.ImportResult subjectResult =
                    gusSubjectImportPersistenceService.importSubjects(subjects);

            for (ImportedVariableData importedVariableData : variableData) {
                gusImportedVariableService.save(importedVariableData.toImportedVariable());
                gusVariableDataCacheService.save(importedVariableData.variableId(), importedVariableData.regions());
            }

            redirectAttributes.addFlashAttribute(
                    "importMessage",
                    "Imported " + subjectResult.totalPersisted()
                            + " subjects (" + subjectResult.created() + " created, "
                            + subjectResult.updated() + " updated, " + subjectResult.skipped() + " skipped)"
                            + " and " + variableData.size() + " variable data caches."
            );
        } catch (IOException | IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "importError",
                    "Could not import JSON: " + exception.getMessage()
            );
        }

        return "redirect:/indicators/import";
    }

    @GetMapping("/indicators/import")
    public String indicatorsImport() {
        return "indicator-import";
    }

    @GetMapping("/indicators/{subjectId}/variables")
    public String variables(
            @PathVariable String subjectId,
            Model model
    ) {
        GUSSubject subject = gusSubjectRepository.findByGusId(subjectId)
                .orElse(null);
        List<GUSVariableDto> variables = getVariablesForSubject(subjectId, model);

        model.addAttribute("subjectId", subjectId);
        model.addAttribute("subject", subject);
        model.addAttribute("variables", variables);

        return "variables";
    }

    private List<GUSVariableDto> getVariablesForSubject(String subjectId, Model model) {
        try {
            return gusClient.getAllVariables(subjectId);
        } catch (RuntimeException exception) {
            List<GUSVariableDto> importedVariables = gusImportedVariableService.getBySubjectId(subjectId);

            if (!importedVariables.isEmpty()) {
                model.addAttribute("variableMessage", "Loaded imported variables from database.");
                return importedVariables;
            }

            model.addAttribute(
                    "variableError",
                    "Could not load variables for subject " + subjectId
                            + ". Ensure this subject id exists in the GUS system."
            );
            return List.of();
        }
    }

    @GetMapping("/indicators/variables/{variableId}/data")
    public String variableData(
            @PathVariable String variableId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) String variableName,
            @RequestParam(required = false) String variableSubname,
            @RequestParam(required = false) String jobId,
            Model model
    ) {
        VariableDataJob job = getOrStartVariableDataJob(variableId, subjectId, jobId);
        boolean completed = "completed".equals(job.getState());
        List<GUSDataRegionDto> regions = completed ? job.getRegions() : List.of();
        Map<Integer, GUSDataAttribute> attributeById = completed ? getAttributeById() : Map.of();

        model.addAttribute("variableId", variableId);
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("subjectName", subjectName);
        model.addAttribute("variableName", variableName);
        model.addAttribute("variableSubname", variableSubname);
        model.addAttribute("loading", !completed);
        model.addAttribute("jobId", job.getId());
        model.addAttribute("jobState", job.getState());
        model.addAttribute("jobMessage", job.getMessage());
        model.addAttribute("dataSource", job.getDataSource());
        model.addAttribute("dataFetchedAt", job.getDataFetchedAt());
        model.addAttribute("regions", regions);
        model.addAttribute("attributeById", attributeById);

        return "variable-data";
    }

    private VariableDataJob getOrStartVariableDataJob(String variableId, String subjectId, String jobId) {
        if (jobId != null && !jobId.isBlank()) {
            return gusVariableDataJobService.get(jobId)
                    .filter(job -> variableId.equals(job.getVariableId()))
                    .orElseGet(() -> gusVariableDataJobService.start(variableId, subjectId));
        }

        return gusVariableDataJobService.start(variableId, subjectId);
    }

    @GetMapping(value = "/indicators/variables/{variableId}/data/export.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<VariableDataExport> variableDataJson(
            @PathVariable String variableId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) String variableName,
            @RequestParam(required = false) String variableSubname,
            @RequestParam(required = false) String jobId
    ) {
        return gusVariableDataJobService.get(jobId)
                .filter(job -> variableId.equals(job.getVariableId()))
                .filter(job -> "completed".equals(job.getState()))
                .map(job -> ResponseEntity.ok(new VariableDataExport(
                        variableId,
                        subjectId,
                        subjectName,
                        variableName,
                        variableSubname,
                        job.getRegions(),
                        getAttributeById()
                )))
                .orElseGet(() -> ResponseEntity.status(409).build());
    }

    @GetMapping("/news")
    public String news(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "newest") String sort,
            Model model
    ) {
        model.addAttribute("articles", newsArticleService.refreshAndGetArticles(q, category, sort));
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("sort", sort);

        return "news";
    }

    @GetMapping(value = "/news/export.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public NewsExport newsJson(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String category,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        List<NewsArticle> articles = newsArticleService.refreshAndGetArticles(q, category, sort);

        return new NewsExport(q, category, sort, articles.size(), articles);
    }

    private Map<Integer, GUSDataAttribute> getAttributeById() {
        return gusDataAttributeService.getAllAttributes().stream()
                .collect(Collectors.toMap(
                        GUSDataAttribute::getId,
                        Function.identity()
                ));
    }

    private List<GUSSubjectDto> readImportedSubjects(JsonNode root) {
        List<GUSSubjectDto> subjects = new ArrayList<>();

        collectSubjects(root, subjects);

        return subjects;
    }

    private List<ImportedVariableData> readImportedVariableData(JsonNode root) {
        List<ImportedVariableData> variableData = new ArrayList<>();

        collectVariableData(root, variableData);

        if (variableData.isEmpty() && readImportedSubjects(root).isEmpty()) {
            throw new IllegalArgumentException("no indicator subjects or variable data found");
        }

        return variableData;
    }

    private void prepareFullImport(List<GUSSubjectDto> subjects, List<ImportedVariableData> variableData) {
        if (variableData.isEmpty()) {
            return;
        }

        Set<String> importedSubjectIds = new HashSet<>();

        for (GUSSubjectDto subject : subjects) {
            if (subject.getId() != null && !subject.getId().isBlank()) {
                importedSubjectIds.add(subject.getId());
            }
        }

        for (ImportedVariableData importedVariableData : variableData) {
            importedVariableData.validate(importedSubjectIds, gusSubjectRepository.findByGusId(importedVariableData.subjectId()).isPresent());

            subjects.stream()
                    .filter(subject -> importedVariableData.subjectId().equals(subject.getId()))
                    .findFirst()
                    .ifPresent(subject -> subject.setHasVariables(true));
        }
    }

    private void collectVariableData(JsonNode node, List<ImportedVariableData> variableData) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isArray()) {
            node.forEach(item -> collectVariableData(item, variableData));
            return;
        }

        if (node.has("variableData")) {
            collectVariableData(node.get("variableData"), variableData);
        }

        if (node.has("variableId") && node.has("regions")) {
            String variableId = textValue(node, "variableId");
            String subjectId = textValue(node, "subjectId");
            String variableName = textValue(node, "variableName", "name");

            if (variableId == null || variableId.isBlank()) {
                throw new IllegalArgumentException("variable data import requires variableId");
            }

            variableData.add(new ImportedVariableData(
                    variableId,
                    subjectId,
                    variableName,
                    textValue(node, "variableSubname", "subname"),
                    integerValue(node, "level"),
                    integerValue(node, "measureUnitId"),
                    textValue(node, "measureUnitName"),
                    objectMapper.convertValue(
                            node.get("regions"),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, GUSDataRegionDto.class)
                    )
            ));
        }
    }

    private void collectSubjects(JsonNode node, List<GUSSubjectDto> subjects) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isArray()) {
            node.forEach(item -> collectSubjects(item, subjects));
            return;
        }

        if (node.has("subjectTree")) {
            collectSubjects(node.get("subjectTree"), subjects);
            return;
        }

        if (node.has("subjects")) {
            collectSubjects(node.get("subjects"), subjects);
            return;
        }

        if (node.has("subject")) {
            subjects.add(toSubjectDto(node.get("subject")));
            collectSubjects(node.get("children"), subjects);
            return;
        }

        if (isSubjectNode(node)) {
            subjects.add(toSubjectDto(node));
        }
    }

    private boolean isSubjectNode(JsonNode node) {
        return node.has("gusId") || node.has("id");
    }

    private GUSSubjectDto toSubjectDto(JsonNode node) {
        GUSSubjectDto subjectDto = new GUSSubjectDto();

        subjectDto.setId(textValue(node, "gusId", "id"));
        subjectDto.setParentId(textValue(node, "parentGusId", "parentId"));
        subjectDto.setName(textValue(node, "name"));
        subjectDto.setHasVariables(booleanValue(node, "hasVariables"));
        subjectDto.setChildren(stringList(node.get("children")));
        subjectDto.setLevels(integerList(node.get("levels")));

        return subjectDto;
    }

    private String textValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);

            if (value != null && value.isValueNode() && !value.isNull()) {
                return value.asText();
            }
        }

        return null;
    }

    private Boolean booleanValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        return value == null || value.isNull() ? null : value.asBoolean();
    }

    private Integer integerValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);

        return value != null && value.canConvertToInt() ? value.asInt() : null;
    }

    private List<String> stringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asText()));

        return values;
    }

    private List<Integer> integerList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }

        List<Integer> values = new ArrayList<>();
        node.forEach(value -> {
            if (value.canConvertToInt()) {
                values.add(value.asInt());
            }
        });

        return values;
    }

    public record IndicatorsExport(
            String query,
            String type,
            String sort,
            long subjectsWithVariablesCount,
            long visibleSubjectsCount,
            List<SubjectNode> subjectTree
    ) {
    }

    public record NewsExport(
            String query,
            String category,
            String sort,
            int count,
            List<NewsArticle> articles
    ) {
    }

    public record VariableDataExport(
            String variableId,
            String subjectId,
            String subjectName,
            String variableName,
            String variableSubname,
            List<GUSDataRegionDto> regions,
            Map<Integer, GUSDataAttribute> attributeById
    ) {
    }

    private record ImportedVariableData(
            String variableId,
            String subjectId,
            String variableName,
            String variableSubname,
            Integer level,
            Integer measureUnitId,
            String measureUnitName,
            List<GUSDataRegionDto> regions
    ) {
        private void validate(Set<String> importedSubjectIds, boolean subjectAlreadyStored) {
            if (subjectId == null || subjectId.isBlank()) {
                throw new IllegalArgumentException("variable data import requires subjectId for " + variableId);
            }

            if (variableName == null || variableName.isBlank()) {
                throw new IllegalArgumentException("variable data import requires variableName for " + variableId);
            }

            if (!importedSubjectIds.contains(subjectId) && !subjectAlreadyStored) {
                throw new IllegalArgumentException("variable data subject " + subjectId + " is not included or stored");
            }

            if (regions == null || regions.isEmpty()) {
                throw new IllegalArgumentException("variable data import requires regions for " + variableId);
            }

            boolean hasEmptyRegion = regions.stream()
                    .anyMatch(region -> region.getId() == null
                            || region.getId().isBlank()
                            || region.getName() == null
                            || region.getName().isBlank()
                            || region.getValues() == null
                            || region.getValues().isEmpty());

            if (hasEmptyRegion) {
                throw new IllegalArgumentException("variable data import requires full region data for " + variableId);
            }
        }

        private GusImportedVariableService.ImportedVariable toImportedVariable() {
            return new GusImportedVariableService.ImportedVariable(
                    variableId,
                    subjectId,
                    variableName,
                    variableSubname,
                    level,
                    measureUnitId,
                    measureUnitName
            );
        }
    }
}
