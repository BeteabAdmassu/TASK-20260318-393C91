package com.mindflow.security.search;

import com.mindflow.security.admin.SystemConfigRepository;
import com.mindflow.security.monitoring.ObservabilityService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final int MAX_RESULTS = 20;
    private static final int MAX_SUGGESTIONS = 8;
    private static final String KEY_RELEVANCE = "search.weight.relevance";
    private static final String KEY_FREQUENCY = "search.weight.frequency";
    private static final String KEY_POPULARITY = "search.weight.popularity";

    private final SystemConfigRepository systemConfigRepository;
    private final TransitStopRepository transitStopRepository;
    private final MeterRegistry meterRegistry;
    private final ObservabilityService observabilityService;

    public SearchService(SystemConfigRepository systemConfigRepository,
                         TransitStopRepository transitStopRepository,
                         MeterRegistry meterRegistry,
                         ObservabilityService observabilityService) {
        this.systemConfigRepository = systemConfigRepository;
        this.transitStopRepository = transitStopRepository;
        this.meterRegistry = meterRegistry;
        this.observabilityService = observabilityService;
    }

    public SearchResponse search(String query) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String normalizedQuery = normalize(query);
        List<ScoredMatch> matches = new ArrayList<>();

        for (TransitStop stop : loadCatalog()) {
            ScoredMatch match = score(stop, normalizedQuery);
            if (match != null) {
                matches.add(match);
            }
        }

        List<ScoredMatch> deduplicated = deduplicateByStop(matches);
        deduplicated.sort(scoreComparator());

        List<SearchResult> results = deduplicated.stream()
                .limit(MAX_RESULTS)
                .map(candidate -> new SearchResult(
                        candidate.stop().routeNumber(),
                        candidate.stop().stopName(),
                        candidate.stop().frequencyPriority(),
                        candidate.stop().stopPopularity(),
                        candidate.rankingScore(),
                        candidate.matchType()))
                .toList();

        List<String> suggestions = deduplicated.stream()
                .limit(MAX_SUGGESTIONS)
                .map(candidate -> candidate.stop().routeNumber() + " - " + candidate.stop().stopName())
                .distinct()
                .toList();

        SearchResponse response = new SearchResponse(query == null ? "" : query, suggestions, results);
        sample.stop(Timer.builder("app.workflow.search.duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));
        observabilityService.recordWorkflowLog("search", "query='" + response.query() + "' results=" + response.results().size());
        return response;
    }

    private List<ScoredMatch> deduplicateByStop(List<ScoredMatch> matches) {
        Map<String, ScoredMatch> byStop = new LinkedHashMap<>();
        for (ScoredMatch match : matches) {
            String key = normalize(match.stop().stopName());
            byStop.merge(key, match, (existing, incoming) ->
                    scoreComparator().compare(existing, incoming) <= 0 ? existing : incoming
            );
        }
        return new ArrayList<>(byStop.values());
    }

    private Comparator<ScoredMatch> scoreComparator() {
        return Comparator
                .comparingLong(ScoredMatch::rankingScore)
                .reversed()
                .thenComparing(match -> match.stop().stopName());
    }

    private ScoredMatch score(TransitStop stop, String query) {
        if (query.isBlank()) {
            long rankingScore = scoreFormula(0, stop.frequencyPriority(), stop.stopPopularity());
            return new ScoredMatch(stop, rankingScore, "popular");
        }

        String route = normalize(stop.routeNumber());
        String stopName = normalize(stop.stopName());
        String pinyin = normalize(stop.pinyin());
        String initials = normalize(stop.initials());
        String keywordCombined = stop.keywords().stream().map(this::normalize).collect(Collectors.joining(" "));

        int relevance = 0;
        String matchType = "keyword";

        if (route.equals(query)) {
            relevance = 6;
            matchType = "route-exact";
        } else if (route.startsWith(query)) {
            relevance = 5;
            matchType = "route-prefix";
        } else if (stopName.startsWith(query)) {
            relevance = 5;
            matchType = "stop-prefix";
        } else if (pinyin.startsWith(query) || initials.startsWith(query)) {
            relevance = 5;
            matchType = "pinyin-prefix";
        } else if (keywordCombined.contains(query)) {
            relevance = 4;
            matchType = "keyword";
        } else if (stopName.contains(query)) {
            relevance = 3;
            matchType = "stop-contains";
        } else if (pinyin.contains(query) || initials.contains(query)) {
            relevance = 3;
            matchType = "pinyin-contains";
        }

        if (relevance == 0) {
            return null;
        }

        long rankingScore = scoreFormula(relevance, stop.frequencyPriority(), stop.stopPopularity());
        return new ScoredMatch(stop, rankingScore, matchType);
    }

    private long scoreFormula(int relevance, int frequencyPriority, int stopPopularity) {
        int relevanceWeight = getWeight(KEY_RELEVANCE, 1_000_000);
        int frequencyWeight = getWeight(KEY_FREQUENCY, 1_000);
        int popularityWeight = getWeight(KEY_POPULARITY, 1);
        return (long) relevance * relevanceWeight
                + (long) frequencyPriority * frequencyWeight
                + (long) stopPopularity * popularityWeight;
    }

    private int getWeight(String key, int fallback) {
        return systemConfigRepository.findByConfigKey(key)
                .map(config -> {
                    try {
                        return Integer.parseInt(config.getConfigValue());
                    } catch (NumberFormatException ignored) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    private List<TransitStop> loadCatalog() {
        return transitStopRepository.findAll().stream()
                .map(this::mapEntity)
                .toList();
    }

    private TransitStop mapEntity(TransitStopEntity row) {
        List<String> keywords = Arrays.stream(row.getKeywords().split(","))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
        return new TransitStop(
                row.getRouteNumber(),
                row.getStopName(),
                keywords,
                row.getPinyin(),
                row.getInitials(),
                row.getFrequencyPriority(),
                row.getStopPopularity()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ScoredMatch(TransitStop stop, long rankingScore, String matchType) {
    }
}
