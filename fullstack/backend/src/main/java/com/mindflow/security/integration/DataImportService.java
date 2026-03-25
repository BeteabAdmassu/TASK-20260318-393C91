package com.mindflow.security.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindflow.security.admin.AdminControlService;
import com.mindflow.security.monitoring.ObservabilityService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DataImportService {

    private static final Pattern FIRST_NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");

    private final ImportJobRepository importJobRepository;
    private final CleanedRecordRepository cleanedRecordRepository;
    private final CleaningAuditRepository cleaningAuditRepository;
    private final StopStructureVersionRepository stopStructureVersionRepository;
    private final AdminControlService adminControlService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final ObservabilityService observabilityService;

    public DataImportService(ImportJobRepository importJobRepository,
                             CleanedRecordRepository cleanedRecordRepository,
                             CleaningAuditRepository cleaningAuditRepository,
                             StopStructureVersionRepository stopStructureVersionRepository,
                             AdminControlService adminControlService,
                             ObjectMapper objectMapper,
                             MeterRegistry meterRegistry,
                             ObservabilityService observabilityService) {
        this.importJobRepository = importJobRepository;
        this.cleanedRecordRepository = cleanedRecordRepository;
        this.cleaningAuditRepository = cleaningAuditRepository;
        this.stopStructureVersionRepository = stopStructureVersionRepository;
        this.adminControlService = adminControlService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.observabilityService = observabilityService;
    }

    @Transactional
    public ImportSummaryResponse ingest(ImportRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        ImportJobEntity job = new ImportJobEntity();
        job.setFormat(request.format());
        job.setSourceName(request.sourceName());
        job.setStatus(ImportStatus.RUNNING);
        job.setTotalRows(0);
        job.setSuccessRows(0);
        job.setFailedRows(0);
        job = importJobRepository.save(job);

        List<String> notes = new ArrayList<>();
        try {
            List<RawImportRecord> parsed = parse(request.format(), request.payload());
            job.setTotalRows(parsed.size());

            for (RawImportRecord raw : parsed) {
                try {
                    cleanAndPersist(job.getId(), raw, notes);
                    job.setSuccessRows(job.getSuccessRows() + 1);
                } catch (Exception rowEx) {
                    job.setFailedRows(job.getFailedRows() + 1);
                    notes.add("Failed row " + raw.sourceRef() + ": " + rowEx.getMessage());
                }
            }

            job.setStatus(ImportStatus.COMPLETED);
        } catch (Exception ex) {
            job.setStatus(ImportStatus.FAILED);
            notes.add("Import failed: " + ex.getMessage());
        }
        job = importJobRepository.save(job);

        sample.stop(Timer.builder("app.workflow.parsing.duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry));
        observabilityService.recordWorkflowLog("parsing", "source=" + request.sourceName() + " status=" + job.getStatus());
        return new ImportSummaryResponse(toJobResponse(job), notes);
    }

    @Transactional(readOnly = true)
    public List<ImportJobResponse> listJobs() {
        return importJobRepository.findAll().stream()
                .map(this::toJobResponse)
                .toList();
    }

    private List<RawImportRecord> parse(ImportFormat format, String payload) throws Exception {
        return switch (format) {
            case JSON -> parseJson(payload);
            case HTML -> parseHtml(payload);
        };
    }

    private List<RawImportRecord> parseJson(String payload) throws Exception {
        List<Map<String, Object>> rows = objectMapper.readValue(payload, new TypeReference<>() {
        });
        List<RawImportRecord> parsed = new ArrayList<>();
        int i = 1;
        for (Map<String, Object> row : rows) {
            Map<String, String> fields = new HashMap<>();
            row.forEach((k, v) -> fields.put(k, v == null ? null : String.valueOf(v)));
            parsed.add(new RawImportRecord("json-row-" + i, fields, objectMapper.writeValueAsString(row)));
            i++;
        }
        return parsed;
    }

    private List<RawImportRecord> parseHtml(String payload) {
        Document doc = Jsoup.parse(payload);
        List<RawImportRecord> parsed = new ArrayList<>();
        int i = 1;
        for (Element row : doc.select("table tr")) {
            if (row.select("th").size() > 0) {
                continue;
            }
            List<Element> cols = row.select("td");
            if (cols.size() < 5) {
                continue;
            }
            Map<String, String> fields = Map.of(
                    "stop", cols.get(0).text(),
                    "address", cols.get(1).text(),
                    "apartment", cols.get(2).text(),
                    "area", cols.get(3).text(),
                    "price", cols.get(4).text()
            );
            parsed.add(new RawImportRecord("html-row-" + i, fields, row.outerHtml()));
            i++;
        }
        return parsed;
    }

    private void cleanAndPersist(Long jobId, RawImportRecord raw, List<String> notes) {
        Map<String, String> map = raw.fields();

        String stop = mapped(map, "stop", "stopName", "station_name");
        String address = mapped(map, "address", "addr", "location");
        String apartment = mapped(map, "apartment", "aptType", "house_type");
        String area = mapped(map, "area", "area_size", "size");
        String price = mapped(map, "price", "rent", "monthly_price");

        String cleanStop = normalizeOrNull("stop_name", stop, raw.sourceRef(), jobId, notes);
        String cleanAddress = normalizeOrNull("address", address, raw.sourceRef(), jobId, notes);
        String cleanApartment = normalizeOrNull("apartment_type", apartment, raw.sourceRef(), jobId, notes);
        String cleanArea = normalizeArea(area, raw.sourceRef(), jobId, notes);
        String cleanPrice = normalizePrice(price, raw.sourceRef(), jobId, notes);

        CleanedRecordEntity entity = new CleanedRecordEntity();
        entity.setImportJobId(jobId);
        entity.setSourceRef(raw.sourceRef());
        entity.setStopName(cleanStop);
        entity.setAddress(cleanAddress);
        entity.setApartmentType(cleanApartment);
        entity.setAreaStandardized(cleanArea);
        entity.setPriceStandardized(cleanPrice);
        entity.setRawSnapshot(raw.rawSnapshot());
        cleanedRecordRepository.save(entity);

        versionFieldIfChanged(cleanStop, "address", cleanAddress, jobId);
        versionFieldIfChanged(cleanStop, "apartment_type", cleanApartment, jobId);
        versionFieldIfChanged(cleanStop, "area_standardized", cleanArea, jobId);
        versionFieldIfChanged(cleanStop, "price_standardized", cleanPrice, jobId);
    }

    private void versionFieldIfChanged(String stopName, String fieldName, String newValue, Long jobId) {
        Optional<StopStructureVersionEntity> latest = stopStructureVersionRepository
                .findTopByStopNameAndFieldNameOrderByVersionNumberDesc(stopName, fieldName);
        String old = latest.map(StopStructureVersionEntity::getNewValue).orElse("NULL");
        if (old.equals(newValue)) {
            return;
        }
        StopStructureVersionEntity version = new StopStructureVersionEntity();
        version.setStopName(stopName);
        version.setFieldName(fieldName);
        version.setOldValue(old);
        version.setNewValue(newValue);
        version.setVersionNumber(latest.map(v -> v.getVersionNumber() + 1).orElse(1));
        version.setImportJobId(jobId);
        stopStructureVersionRepository.save(version);
    }

    private String mapped(Map<String, String> map, String... keys) {
        for (String key : keys) {
            String value = map.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeOrNull(String fieldName, String value, String sourceRef, Long jobId, List<String> notes) {
        String missingMarker = adminControlService.getCleaningRules().missingValueMarker();
        boolean trimEnabled = adminControlService.getCleaningRules().trimEnabled();
        if (value == null || value.isBlank()) {
            audit(jobId, sourceRef, fieldName, "", missingMarker, "MISSING_TO_NULL");
            notes.add("Missing value mapped to " + missingMarker + " for " + fieldName + " from " + sourceRef);
            return missingMarker;
        }
        String cleaned = trimEnabled ? value.trim() : value;
        if (!cleaned.equals(value)) {
            audit(jobId, sourceRef, fieldName, value, cleaned, "TRIMMED");
        }
        return cleaned;
    }

    private String normalizeArea(String value, String sourceRef, Long jobId, List<String> notes) {
        String areaUnit = adminControlService.getCleaningRules().areaUnit();
        String missingMarker = adminControlService.getCleaningRules().missingValueMarker();
        if (value == null || value.isBlank()) {
            audit(jobId, sourceRef, "area", "", missingMarker, "MISSING_TO_NULL");
            notes.add("Missing area mapped to " + missingMarker + " from " + sourceRef);
            return missingMarker;
        }
        String numeric = extractFirstNumber(value);
        String cleaned = numeric == null ? missingMarker : numeric + " " + areaUnit;
        audit(jobId, sourceRef, "area", value, cleaned, "UNIT_STANDARDIZED");
        return cleaned;
    }

    private String normalizePrice(String value, String sourceRef, Long jobId, List<String> notes) {
        String priceUnit = adminControlService.getCleaningRules().priceUnit();
        String missingMarker = adminControlService.getCleaningRules().missingValueMarker();
        if (value == null || value.isBlank()) {
            audit(jobId, sourceRef, "price", "", missingMarker, "MISSING_TO_NULL");
            notes.add("Missing price mapped to " + missingMarker + " from " + sourceRef);
            return missingMarker;
        }
        String numeric = extractFirstNumber(value);
        String cleaned = numeric == null ? missingMarker : numeric + " " + priceUnit;
        audit(jobId, sourceRef, "price", value, cleaned, "UNIT_STANDARDIZED");
        return cleaned;
    }

    private String extractFirstNumber(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = FIRST_NUMBER.matcher(value.toLowerCase(Locale.ROOT));
        if (!matcher.find()) {
            return null;
        }
        return matcher.group();
    }

    private void audit(Long jobId, String sourceRef, String fieldName, String raw, String cleaned, String action) {
        CleaningAuditEntity log = new CleaningAuditEntity();
        log.setImportJobId(jobId);
        log.setSourceRef(sourceRef);
        log.setFieldName(fieldName);
        log.setRawValue(raw == null ? "" : raw);
        log.setCleanedValue(cleaned == null ? "" : cleaned);
        log.setAction(action);
        cleaningAuditRepository.save(log);
    }

    private ImportJobResponse toJobResponse(ImportJobEntity job) {
        return new ImportJobResponse(
                job.getId(),
                job.getStatus(),
                job.getSourceName(),
                job.getTotalRows(),
                job.getSuccessRows(),
                job.getFailedRows(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
