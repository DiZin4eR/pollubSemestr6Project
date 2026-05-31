package com.ecodatahub.gus.service;

import com.ecodatahub.gus.domain.GUSSubject;
import com.ecodatahub.gus.repository.GUSSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GusSubjectTreeService {

    private static final String ROOT_SUBJECT_PREFIX = "K";

    private final GUSSubjectRepository gusSubjectRepository;

    public SubjectTreeResult getSubjectTree(String query, String type, String sort) {
        List<SubjectNode> subjectTree = getFilteredSubjectTree(query, type, sort);

        return new SubjectTreeResult(
                subjectTree,
                gusSubjectRepository.countByHasVariablesTrue(),
                countSubjectNodes(subjectTree)
        );
    }

    private List<SubjectNode> getFilteredSubjectTree(String query, String type, String sort) {
        List<GUSSubject> subjects = gusSubjectRepository.findAll(
                Sort.by("parentGusId")
                        .ascending()
                        .and(Sort.by("gusId").ascending())
        );

        return filterSubjectTree(buildSubjectTree(subjects, sort), query, type);
    }

    private List<SubjectNode> buildSubjectTree(List<GUSSubject> subjects, String sort) {
        Map<String, GUSSubject> subjectByGusId = subjects.stream()
                .collect(Collectors.toMap(
                        GUSSubject::getGusId,
                        Function.identity(),
                        (first, second) -> first
                ));
        Map<String, List<GUSSubject>> subjectsByParentId = subjects.stream()
                .filter(subject -> subject.getParentGusId() != null)
                .collect(Collectors.groupingBy(GUSSubject::getParentGusId));

        return subjects.stream()
                .filter(this::isRootSubject)
                .sorted(subjectComparator(sort))
                .map(subject -> toNode(subject, subjectByGusId, subjectsByParentId, new HashSet<>(), sort))
                .toList();
    }

    private boolean isRootSubject(GUSSubject subject) {
        return subject.getParentGusId() == null
                && subject.getGusId() != null
                && subject.getGusId().startsWith(ROOT_SUBJECT_PREFIX);
    }

    private SubjectNode toNode(
            GUSSubject subject,
            Map<String, GUSSubject> subjectByGusId,
            Map<String, List<GUSSubject>> subjectsByParentId,
            Set<String> visitedSubjectIds,
            String sort
    ) {
        if (!visitedSubjectIds.add(subject.getGusId())) {
            return new SubjectNode(subject, List.of());
        }

        List<GUSSubject> childSubjects = getChildSubjects(subject, subjectByGusId, subjectsByParentId, sort);
        List<SubjectNode> children = childSubjects.stream()
                .map(child -> toNode(child, subjectByGusId, subjectsByParentId, new HashSet<>(visitedSubjectIds), sort))
                .toList();

        return new SubjectNode(subject, children);
    }

    private List<GUSSubject> getChildSubjects(
            GUSSubject subject,
            Map<String, GUSSubject> subjectByGusId,
            Map<String, List<GUSSubject>> subjectsByParentId,
            String sort
    ) {
        if (subject.getChildren() != null && !subject.getChildren().isEmpty()) {
            return subject.getChildren().stream()
                    .map(subjectByGusId::get)
                    .filter(child -> child != null)
                    .sorted(subjectComparator(sort))
                    .toList();
        }

        return subjectsByParentId.getOrDefault(subject.getGusId(), new ArrayList<>()).stream()
                .sorted(subjectComparator(sort))
                .toList();
    }

    private Comparator<GUSSubject> subjectComparator(String sort) {
        if ("name".equals(sort)) {
            return Comparator.comparing(
                            GUSSubject::getName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                    )
                    .thenComparing(GUSSubject::getGusId, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        }

        return Comparator.comparing(GUSSubject::getGusId, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    private List<SubjectNode> filterSubjectTree(List<SubjectNode> nodes, String query, String type) {
        return nodes.stream()
                .map(node -> filterSubjectNode(node, query, type))
                .filter(node -> node != null)
                .toList();
    }

    private SubjectNode filterSubjectNode(SubjectNode node, String query, String type) {
        List<SubjectNode> filteredChildren = filterSubjectTree(node.children(), query, type);

        if (matchesSubject(node.subject(), query, type) || !filteredChildren.isEmpty()) {
            return new SubjectNode(node.subject(), filteredChildren);
        }

        return null;
    }

    private boolean matchesSubject(GUSSubject subject, String query, String type) {
        return matchesSubjectType(subject, type) && matchesSubjectQuery(subject, query);
    }

    private boolean matchesSubjectType(GUSSubject subject, String type) {
        if ("variables".equals(type)) {
            return Boolean.TRUE.equals(subject.getHasVariables());
        }

        if ("categories".equals(type)) {
            return !Boolean.TRUE.equals(subject.getHasVariables());
        }

        return true;
    }

    private boolean matchesSubjectQuery(GUSSubject subject, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT);

        return contains(subject.getGusId(), normalizedQuery)
                || contains(subject.getName(), normalizedQuery);
    }

    private boolean contains(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private long countSubjectNodes(List<SubjectNode> nodes) {
        return nodes.stream()
                .mapToLong(node -> 1 + countSubjectNodes(node.children()))
                .sum();
    }

    public record SubjectTreeResult(
            List<SubjectNode> subjectTree,
            long subjectsWithVariablesCount,
            long visibleSubjectsCount
    ) {
    }

    public record SubjectNode(
            GUSSubject subject,
            List<SubjectNode> children
    ) {
    }
}
