package com.mindflow.security.integration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/integration")
@Validated
public class DataIntegrationController {

    private final DataImportService dataImportService;
    private final CleaningAuditRepository cleaningAuditRepository;
    private final StopStructureVersionRepository stopStructureVersionRepository;

    public DataIntegrationController(DataImportService dataImportService,
                                     CleaningAuditRepository cleaningAuditRepository,
                                     StopStructureVersionRepository stopStructureVersionRepository) {
        this.dataImportService = dataImportService;
        this.cleaningAuditRepository = cleaningAuditRepository;
        this.stopStructureVersionRepository = stopStructureVersionRepository;
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportSummaryResponse> importData(@Valid @RequestBody ImportRequest request) {
        return ResponseEntity.ok(dataImportService.ingest(request));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ImportJobResponse>> jobs() {
        return ResponseEntity.ok(dataImportService.listJobs());
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CleaningAuditResponse>> audit(@RequestParam("jobId") @Positive Long jobId) {
        List<CleaningAuditResponse> rows = cleaningAuditRepository.findByImportJobIdOrderByIdAsc(jobId)
                .stream()
                .map(a -> new CleaningAuditResponse(
                        a.getSourceRef(),
                        a.getFieldName(),
                        a.getRawValue(),
                        a.getCleanedValue(),
                        a.getAction()))
                .toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/versions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StopVersionResponse>> versions(
            @RequestParam(name = "stopName", required = false) String stopName,
            @RequestParam(name = "jobId", required = false) @Positive Long jobId) {
        List<StopStructureVersionEntity> rows;
        if (stopName != null && !stopName.isBlank()) {
            rows = stopStructureVersionRepository.findByStopNameOrderByVersionNumberDesc(stopName);
        } else if (jobId != null) {
            rows = stopStructureVersionRepository.findByImportJobIdOrderByIdAsc(jobId);
        } else {
            rows = stopStructureVersionRepository.findAll();
        }
        return ResponseEntity.ok(rows.stream()
                .map(v -> new StopVersionResponse(
                        v.getStopName(),
                        v.getFieldName(),
                        v.getOldValue(),
                        v.getNewValue(),
                        v.getVersionNumber(),
                        v.getImportJobId()))
                .toList());
    }
}
