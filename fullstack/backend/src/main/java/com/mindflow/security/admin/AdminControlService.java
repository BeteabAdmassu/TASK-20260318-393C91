package com.mindflow.security.admin;

import com.mindflow.security.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminControlService {

    private static final String KEY_RELEVANCE = "search.weight.relevance";
    private static final String KEY_FREQUENCY = "search.weight.frequency";
    private static final String KEY_POPULARITY = "search.weight.popularity";
    private static final String KEY_CLEAN_AREA_UNIT = "cleaning.rule.area.unit";
    private static final String KEY_CLEAN_PRICE_UNIT = "cleaning.rule.price.unit";
    private static final String KEY_CLEAN_MISSING_MARKER = "cleaning.rule.missing.marker";
    private static final String KEY_CLEAN_TRIM_ENABLED = "cleaning.rule.trim.enabled";

    private final SystemConfigRepository systemConfigRepository;
    private final NotificationTemplateRepository templateRepository;
    private final FieldDictionaryRepository dictionaryRepository;

    public AdminControlService(SystemConfigRepository systemConfigRepository,
                               NotificationTemplateRepository templateRepository,
                               FieldDictionaryRepository dictionaryRepository) {
        this.systemConfigRepository = systemConfigRepository;
        this.templateRepository = templateRepository;
        this.dictionaryRepository = dictionaryRepository;
    }

    @Transactional(readOnly = true)
    public RuleWeightsResponse getRuleWeights() {
        int relevance = getInt(KEY_RELEVANCE, 1_000_000);
        int frequency = getInt(KEY_FREQUENCY, 1_000);
        int popularity = getInt(KEY_POPULARITY, 1);
        return new RuleWeightsResponse(relevance, frequency, popularity);
    }

    @Transactional
    public RuleWeightsResponse updateRuleWeights(RuleWeightsRequest request) {
        upsert(KEY_RELEVANCE, request.relevanceWeight().toString());
        upsert(KEY_FREQUENCY, request.frequencyWeight().toString());
        upsert(KEY_POPULARITY, request.popularityWeight().toString());
        return getRuleWeights();
    }

    @Transactional(readOnly = true)
    public CleaningRuleSetResponse getCleaningRules() {
        return new CleaningRuleSetResponse(
                getString(KEY_CLEAN_AREA_UNIT, "㎡"),
                getString(KEY_CLEAN_PRICE_UNIT, "yuan/month"),
                getString(KEY_CLEAN_MISSING_MARKER, "NULL"),
                Boolean.parseBoolean(getString(KEY_CLEAN_TRIM_ENABLED, "true"))
        );
    }

    @Transactional
    public CleaningRuleSetResponse updateCleaningRules(CleaningRuleSetRequest request) {
        upsert(KEY_CLEAN_AREA_UNIT, request.areaUnit());
        upsert(KEY_CLEAN_PRICE_UNIT, request.priceUnit());
        upsert(KEY_CLEAN_MISSING_MARKER, request.missingValueMarker());
        upsert(KEY_CLEAN_TRIM_ENABLED, request.trimEnabled().toString());
        return getCleaningRules();
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listTemplates() {
        return templateRepository.findAll().stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @Transactional
    public TemplateResponse createTemplate(TemplateRequest request) {
        NotificationTemplateEntity entity = new NotificationTemplateEntity();
        entity.setTemplateKey(request.templateKey());
        entity.setSubject(request.subject());
        entity.setBody(request.body());
        return toTemplateResponse(templateRepository.save(entity));
    }

    @Transactional
    public TemplateResponse updateTemplate(Long id, TemplateRequest request) {
        NotificationTemplateEntity entity = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
        entity.setTemplateKey(request.templateKey());
        entity.setSubject(request.subject());
        entity.setBody(request.body());
        return toTemplateResponse(templateRepository.save(entity));
    }

    @Transactional
    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<DictionaryResponse> listDictionary(String category) {
        List<FieldDictionaryEntity> rows = (category == null || category.isBlank())
                ? dictionaryRepository.findAll()
                : dictionaryRepository.findByCategoryOrderByCodeAsc(category);
        return rows.stream().map(this::toDictionaryResponse).toList();
    }

    @Transactional
    public DictionaryResponse createDictionary(DictionaryRequest request) {
        FieldDictionaryEntity entity = new FieldDictionaryEntity();
        entity.setCategory(request.category());
        entity.setCode(request.code());
        entity.setItemValue(request.value());
        entity.setEnabled(request.enabled());
        return toDictionaryResponse(dictionaryRepository.save(entity));
    }

    @Transactional
    public DictionaryResponse updateDictionary(Long id, DictionaryRequest request) {
        FieldDictionaryEntity entity = dictionaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dictionary item not found"));
        entity.setCategory(request.category());
        entity.setCode(request.code());
        entity.setItemValue(request.value());
        entity.setEnabled(request.enabled());
        return toDictionaryResponse(dictionaryRepository.save(entity));
    }

    @Transactional
    public void deleteDictionary(Long id) {
        dictionaryRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TemplateResponse resolveTemplate(String templateKey, String fallbackSubject, String fallbackBody) {
        return templateRepository.findByTemplateKey(templateKey)
                .map(this::toTemplateResponse)
                .orElse(new TemplateResponse(null, templateKey, fallbackSubject, fallbackBody));
    }

    @Transactional(readOnly = true)
    public Optional<String> resolveDictionaryValue(String category, String code) {
        if (category == null || category.isBlank() || code == null || code.isBlank()) {
            return Optional.empty();
        }
        return dictionaryRepository.findByCategoryAndCodeAndEnabledTrue(category, code)
                .map(FieldDictionaryEntity::getItemValue);
    }

    @Transactional(readOnly = true)
    public Map<String, String> dictionaryMap(String category) {
        if (category == null || category.isBlank()) {
            return Map.of();
        }
        return dictionaryRepository.findByCategoryAndEnabledTrueOrderByCodeAsc(category)
                .stream()
                .collect(Collectors.toMap(FieldDictionaryEntity::getCode, FieldDictionaryEntity::getItemValue, (left, right) -> right));
    }

    private int getInt(String key, int fallback) {
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfigEntity::getConfigValue)
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                        return fallback;
                    }
                })
                .orElse(fallback);
    }

    private String getString(String key, String fallback) {
        return systemConfigRepository.findByConfigKey(key)
                .map(SystemConfigEntity::getConfigValue)
                .orElse(fallback);
    }

    private void upsert(String key, String value) {
        SystemConfigEntity entity = systemConfigRepository.findByConfigKey(key).orElseGet(() -> {
            SystemConfigEntity created = new SystemConfigEntity();
            created.setConfigKey(key);
            return created;
        });
        entity.setConfigValue(value);
        systemConfigRepository.save(entity);
    }

    private TemplateResponse toTemplateResponse(NotificationTemplateEntity entity) {
        return new TemplateResponse(entity.getId(), entity.getTemplateKey(), entity.getSubject(), entity.getBody());
    }

    private DictionaryResponse toDictionaryResponse(FieldDictionaryEntity entity) {
        return new DictionaryResponse(
                entity.getId(),
                entity.getCategory(),
                entity.getCode(),
                entity.getItemValue(),
                entity.isEnabled()
        );
    }
}
