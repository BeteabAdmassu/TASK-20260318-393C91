package com.mindflow.security.integration;

import com.mindflow.security.admin.AdminControlService;
import com.mindflow.security.admin.CleaningRuleSetRequest;
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
}
