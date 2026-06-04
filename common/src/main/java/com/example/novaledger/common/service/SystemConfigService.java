package com.example.novaledger.common.service;

import com.example.novaledger.common.entity.SystemConfig;
import com.example.novaledger.common.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private static final String CACHE_NAME = "systemConfigs";

    private final SystemConfigRepository systemConfigRepository;

    @Cacheable(value = CACHE_NAME, key = "#key")
    public String getString(String key) {
        return systemConfigRepository.findById(key)
                .map(SystemConfig::getConfigValue)
                .orElseThrow(() -> new IllegalArgumentException("SystemConfig key not found: " + key));
    }

    @Cacheable(value = CACHE_NAME, key = "#key")
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

    @Cacheable(value = CACHE_NAME, key = "#key")
    public int getInteger(String key) {
        return Integer.parseInt(getString(key));
    }

    @CacheEvict(value = CACHE_NAME, key = "#key")
    @Transactional
    public void set(String key, String newValue, String updatedBy) {
        SystemConfig config = systemConfigRepository.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("SystemConfig key not found: " + key));
        config.updateValue(newValue, updatedBy);
        systemConfigRepository.save(config);
        log.info("action=SYSTEM_CONFIG_UPDATED key={} updatedBy={}", key, updatedBy);
    }
}
