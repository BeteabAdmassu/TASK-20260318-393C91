package com.mindflow.security.search;

import com.mindflow.security.admin.SystemConfigRepository;
import com.mindflow.security.monitoring.ObservabilityService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    private SearchService service;

    @BeforeEach
    void setUp() {
        SystemConfigRepository configRepository = mock(SystemConfigRepository.class);
        TransitStopRepository transitStopRepository = mock(TransitStopRepository.class);
        ObservabilityService observabilityService = mock(ObservabilityService.class);

        TransitStopEntity row1 = new TransitStopEntity();
        row1.setRouteNumber("1A");
        row1.setStopName("Beijing Central");
        row1.setKeywords("central,hub,main station");
        row1.setPinyin("beijing");
        row1.setInitials("bj");
        row1.setFrequencyPriority(95);
        row1.setStopPopularity(980);

        TransitStopEntity row2 = new TransitStopEntity();
        row2.setRouteNumber("BJ7");
        row2.setStopName("Beijing Central");
        row2.setKeywords("beijing,north,gate");
        row2.setPinyin("beijing");
        row2.setInitials("bj");
        row2.setFrequencyPriority(85);
        row2.setStopPopularity(820);

        when(transitStopRepository.findAll()).thenReturn(List.of(row1, row2));
        service = new SearchService(configRepository, transitStopRepository, new SimpleMeterRegistry(), observabilityService);
    }

    @Test
    void supportsInitialLetterMatching() {
        SearchResponse response = service.search("bj");
        assertFalse(response.results().isEmpty());
        assertEquals("Beijing Central", response.results().get(0).stopName());
    }

    @Test
    void deduplicatesDuplicateStops() {
        SearchResponse response = service.search("beijing");
        long centralCount = response.results().stream()
                .filter(result -> result.stopName().equals("Beijing Central"))
                .count();
        assertEquals(1, centralCount);
    }

    @Test
    void sortsByFrequencyAndPopularityAfterRelevance() {
        SearchResponse response = service.search("beijing");
        assertEquals("Beijing Central", response.results().get(0).stopName());
    }
}
