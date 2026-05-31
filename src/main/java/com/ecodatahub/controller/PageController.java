package com.ecodatahub.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.ecodatahub.gus.domain.GUSDataAttribute;
import com.ecodatahub.gus.domain.GUSSubject;
import com.ecodatahub.news.domain.NewsArticle;
import com.ecodatahub.gus.api.dto.GUSDataRegionDto;
import com.ecodatahub.gus.api.dto.GUSVariableDto;
import com.ecodatahub.gus.service.GusSubjectTreeService;
import com.ecodatahub.gus.service.GusSubjectTreeService.SubjectNode;
import com.ecodatahub.gus.service.GusSubjectTreeService.SubjectTreeResult;
import com.ecodatahub.gus.service.GusDataAttributeService;
import com.ecodatahub.gus.service.GusVariableDataJobService;
import com.ecodatahub.gus.service.GusVariableDataJobService.VariableDataJob;
import com.ecodatahub.gus.repository.GUSSubjectRepository;
import com.ecodatahub.gus.api.GusApiClient;
import com.ecodatahub.news.service.NewsArticleService;

import java.util.List;
import java.util.Map;
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

    @GetMapping("/indicators/{subjectId}/variables")
    public String variables(
            @PathVariable String subjectId,
            Model model
    ) {
        GUSSubject subject = gusSubjectRepository.findByGusId(subjectId)
                .orElse(null);
        List<GUSVariableDto> variables = gusClient.getAllVariables(subjectId);

        model.addAttribute("subjectId", subjectId);
        model.addAttribute("subject", subject);
        model.addAttribute("variables", variables);

        return "variables";
    }

    @GetMapping("/indicators/variables/{variableId}/data")
    public String variableData(
            @PathVariable String variableId,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) String variableName,
            @RequestParam(required = false) String variableSubname,
            @RequestParam(required = false) String jobId,
            Model model
    ) {
        VariableDataJob job = getOrStartVariableDataJob(variableId, jobId);
        boolean completed = "completed".equals(job.getState());
        List<GUSDataRegionDto> regions = completed ? job.getRegions() : List.of();
        Map<Integer, GUSDataAttribute> attributeById = completed ? getAttributeById() : Map.of();

        model.addAttribute("variableId", variableId);
        model.addAttribute("subjectName", subjectName);
        model.addAttribute("variableName", variableName);
        model.addAttribute("variableSubname", variableSubname);
        model.addAttribute("loading", !completed);
        model.addAttribute("jobId", job.getId());
        model.addAttribute("jobState", job.getState());
        model.addAttribute("jobMessage", job.getMessage());
        model.addAttribute("regions", regions);
        model.addAttribute("attributeById", attributeById);

        return "variable-data";
    }

    private VariableDataJob getOrStartVariableDataJob(String variableId, String jobId) {
        if (jobId != null && !jobId.isBlank()) {
            return gusVariableDataJobService.get(jobId)
                    .filter(job -> variableId.equals(job.getVariableId()))
                    .orElseGet(() -> gusVariableDataJobService.start(variableId));
        }

        return gusVariableDataJobService.start(variableId);
    }

    @GetMapping(value = "/indicators/variables/{variableId}/data/export.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<VariableDataExport> variableDataJson(
            @PathVariable String variableId,
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
            String subjectName,
            String variableName,
            String variableSubname,
            List<GUSDataRegionDto> regions,
            Map<Integer, GUSDataAttribute> attributeById
    ) {
    }
}
