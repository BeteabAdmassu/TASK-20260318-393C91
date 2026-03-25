package com.mindflow.security.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/control")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminControlController {

    private final AdminControlService service;

    public AdminControlController(AdminControlService service) {
        this.service = service;
    }

    @GetMapping("/search-weights")
    public ResponseEntity<RuleWeightsResponse> getWeights() {
        return ResponseEntity.ok(service.getRuleWeights());
    }

    @PutMapping("/search-weights")
    public ResponseEntity<RuleWeightsResponse> updateWeights(@Valid @RequestBody RuleWeightsRequest request) {
        return ResponseEntity.ok(service.updateRuleWeights(request));
    }

    @GetMapping("/cleaning-rules")
    public ResponseEntity<CleaningRuleSetResponse> getCleaningRules() {
        return ResponseEntity.ok(service.getCleaningRules());
    }

    @PutMapping("/cleaning-rules")
    public ResponseEntity<CleaningRuleSetResponse> updateCleaningRules(@Valid @RequestBody CleaningRuleSetRequest request) {
        return ResponseEntity.ok(service.updateCleaningRules(request));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<TemplateResponse>> listTemplates() {
        return ResponseEntity.ok(service.listTemplates());
    }

    @PostMapping("/templates")
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTemplate(request));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(@PathVariable @Positive Long id,
                                                           @Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.ok(service.updateTemplate(id, request));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable @Positive Long id) {
        service.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dictionaries")
    public ResponseEntity<List<DictionaryResponse>> listDictionary(
            @RequestParam(name = "category", required = false) String category) {
        return ResponseEntity.ok(service.listDictionary(category));
    }

    @PostMapping("/dictionaries")
    public ResponseEntity<DictionaryResponse> createDictionary(@Valid @RequestBody DictionaryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDictionary(request));
    }

    @PutMapping("/dictionaries/{id}")
    public ResponseEntity<DictionaryResponse> updateDictionary(@PathVariable @Positive Long id,
                                                               @Valid @RequestBody DictionaryRequest request) {
        return ResponseEntity.ok(service.updateDictionary(id, request));
    }

    @DeleteMapping("/dictionaries/{id}")
    public ResponseEntity<Void> deleteDictionary(@PathVariable @Positive Long id) {
        service.deleteDictionary(id);
        return ResponseEntity.noContent().build();
    }
}
