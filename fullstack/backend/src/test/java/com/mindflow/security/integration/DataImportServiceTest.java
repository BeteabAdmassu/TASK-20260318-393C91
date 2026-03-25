package com.mindflow.security.integration;

import com.mindflow.security.admin.AdminControlService;
import com.mindflow.security.admin.CleaningRuleSetRequest;
import com.mindflow.security.admin.DictionaryRequest;
import com.mindflow.security.search.TransitStopRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class DataImportServiceTest {

    @Autowired
    private DataImportService dataImportService;

    @Autowired
    private CleanedRecordRepository cleanedRecordRepository;

    @Autowired
    private AdminControlService adminControlService;

    @Autowired
    private TransitStopRepository transitStopRepository;

    @Test
    void importRespectsConfigurableCleaningRules() {
        adminControlService.updateCleaningRules(new CleaningRuleSetRequest("sqm", "cny/month", "MISSING", true));

        String payload = """
                [
                  {"stop":"Central Stop","address":"Road 1","apartment":"Studio","area":"88 m2","price":"2200 rmb"}
                ]
                """;

        ImportSummaryResponse summary = dataImportService.ingest(new ImportRequest(ImportFormat.JSON, "test-json", payload));
        assertTrue(summary.job().successRows() >= 1);

        CleanedRecordEntity row = cleanedRecordRepository.findAll().stream()
                .filter(record -> record.getImportJobId().equals(summary.job().jobId()))
                .findFirst()
                .orElse(null);

        assertNotNull(row);
        assertEquals("88 sqm", row.getAreaStandardized());
        assertEquals("2200 cny/month", row.getPriceStandardized());
    }

    @Test
    void importFeedsSearchCatalogAndAppliesDictionaryMapping() {
        adminControlService.createDictionary(new DictionaryRequest("apartment_type", "Studio", "Apartment-Studio", true));
        adminControlService.createDictionary(new DictionaryRequest("address", "Road 9", "North District Road 9", true));

        String payload = """
                [
                  {"stop":"North Garden","address":"Road 9","apartment":"Studio","area":"66 m2","price":"1900 rmb"}
                ]
                """;

        ImportSummaryResponse summary = dataImportService.ingest(new ImportRequest(ImportFormat.JSON, "catalog-json", payload));
        assertTrue(summary.job().successRows() >= 1);

        boolean existsInTransitStops = transitStopRepository.findAll().stream()
                .anyMatch(stop -> "North Garden".equals(stop.getStopName())
                        && stop.getKeywords().contains("apartment-studio")
                        && stop.getKeywords().contains("north district road 9"));

        assertTrue(existsInTransitStops);
    }
}
