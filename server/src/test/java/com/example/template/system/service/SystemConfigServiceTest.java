package com.example.template.system.service;

import com.example.template.common.BusinessException;
import com.example.template.common.ResultCode;
import com.example.template.system.dto.ConfigBatchUpdateRequest;
import com.example.template.system.dto.ConfigCreateRequest;
import com.example.template.system.dto.ConfigItem;
import com.example.template.system.dto.ConfigMetaUpdateRequest;
import com.example.template.system.dto.ConfigUpdateItem;
import com.example.template.system.repository.SysConfigRepository;
import com.example.template.system.service.impl.SystemConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceTest {

    @Mock
    private SysConfigRepository sysConfigRepository;

    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUp() {
        systemConfigService = new SystemConfigServiceImpl(sysConfigRepository);
    }

    @Test
    void batchUpdate_success() {
        ConfigItem item = new ConfigItem();
        item.setConfigKey("site.name");
        item.setValueType("STRING");
        item.setEditable(1);
        when(sysConfigRepository.findByKey("site.name")).thenReturn(Optional.of(item));
        when(sysConfigRepository.updateValue("site.name", "新站点")).thenReturn(1);
        when(sysConfigRepository.findAllValues()).thenReturn(Map.of("site.name", "新站点"));

        ConfigBatchUpdateRequest request = new ConfigBatchUpdateRequest();
        ConfigUpdateItem update = new ConfigUpdateItem();
        update.setConfigKey("site.name");
        update.setConfigValue("新站点");
        request.setItems(List.of(update));

        systemConfigService.batchUpdate(request);

        verify(sysConfigRepository).updateValue("site.name", "新站点");
    }

    @Test
    void batchUpdate_skipsUnchangedValue() {
        ConfigItem item = new ConfigItem();
        item.setConfigKey("site.name");
        item.setConfigValue("管理后台");
        item.setValueType("STRING");
        item.setEditable(1);
        when(sysConfigRepository.findByKey("site.name")).thenReturn(Optional.of(item));
        when(sysConfigRepository.findAllValues()).thenReturn(Map.of("site.name", "管理后台"));

        ConfigBatchUpdateRequest request = new ConfigBatchUpdateRequest();
        ConfigUpdateItem update = new ConfigUpdateItem();
        update.setConfigKey("site.name");
        update.setConfigValue("管理后台");
        request.setItems(List.of(update));

        systemConfigService.batchUpdate(request);

        verify(sysConfigRepository, never()).updateValue(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void batchUpdate_invalidBoolean() {
        ConfigItem item = new ConfigItem();
        item.setConfigKey("auth.login.captcha_enabled");
        item.setValueType("BOOLEAN");
        item.setEditable(1);
        when(sysConfigRepository.findByKey("auth.login.captcha_enabled")).thenReturn(Optional.of(item));

        ConfigBatchUpdateRequest request = new ConfigBatchUpdateRequest();
        ConfigUpdateItem update = new ConfigUpdateItem();
        update.setConfigKey("auth.login.captcha_enabled");
        update.setConfigValue("yes");
        request.setItems(List.of(update));

        BusinessException ex = assertThrows(BusinessException.class, () -> systemConfigService.batchUpdate(request));
        assertEquals(ResultCode.CONFIG_VALUE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void batchUpdate_invalidJsonSyntax() {
        ConfigItem item = new ConfigItem();
        item.setConfigKey("custom.theme");
        item.setValueType("JSON");
        item.setEditable(1);
        when(sysConfigRepository.findByKey("custom.theme")).thenReturn(Optional.of(item));

        ConfigBatchUpdateRequest request = new ConfigBatchUpdateRequest();
        ConfigUpdateItem update = new ConfigUpdateItem();
        update.setConfigKey("custom.theme");
        update.setConfigValue("{invalid}");
        request.setItems(List.of(update));

        BusinessException ex = assertThrows(BusinessException.class, () -> systemConfigService.batchUpdate(request));
        assertEquals(ResultCode.CONFIG_VALUE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void batchUpdate_jsonTrimsWhitespace() {
        ConfigItem item = new ConfigItem();
        item.setConfigKey("custom.theme");
        item.setConfigValue("{}");
        item.setValueType("JSON");
        item.setEditable(1);
        when(sysConfigRepository.findByKey("custom.theme")).thenReturn(Optional.of(item));
        when(sysConfigRepository.updateValue("custom.theme", "{\"enabled\":true}")).thenReturn(1);
        when(sysConfigRepository.findAllValues()).thenReturn(Map.of("custom.theme", "{\"enabled\":true}"));

        ConfigBatchUpdateRequest request = new ConfigBatchUpdateRequest();
        ConfigUpdateItem update = new ConfigUpdateItem();
        update.setConfigKey("custom.theme");
        update.setConfigValue("  {\"enabled\":true}  ");
        request.setItems(List.of(update));

        systemConfigService.batchUpdate(request);

        verify(sysConfigRepository).updateValue("custom.theme", "{\"enabled\":true}");
    }

    @Test
    void create_invalidJson() {
        when(sysConfigRepository.findByKey("custom.theme")).thenReturn(Optional.empty());

        ConfigCreateRequest request = new ConfigCreateRequest();
        request.setConfigKey("custom.theme");
        request.setConfigName("主题");
        request.setConfigGroup("custom");
        request.setValueType("JSON");
        request.setConfigValue("{bad}");

        BusinessException ex = assertThrows(BusinessException.class, () -> systemConfigService.create(request));
        assertEquals(ResultCode.CONFIG_VALUE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void batchUpdate_passwordMinLengthOutOfRange() {
        ConfigItem item = new ConfigItem();
        item.setConfigKey("auth.password.min_length");
        item.setValueType("NUMBER");
        item.setEditable(1);
        when(sysConfigRepository.findByKey("auth.password.min_length")).thenReturn(Optional.of(item));

        ConfigBatchUpdateRequest request = new ConfigBatchUpdateRequest();
        ConfigUpdateItem update = new ConfigUpdateItem();
        update.setConfigKey("auth.password.min_length");
        update.setConfigValue("999");
        request.setItems(List.of(update));

        BusinessException ex = assertThrows(BusinessException.class, () -> systemConfigService.batchUpdate(request));
        assertEquals(ResultCode.CONFIG_VALUE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void batchUpdate_siteLogoInvalidScheme() {
        ConfigItem item = new ConfigItem();
        item.setConfigKey("site.logo");
        item.setValueType("STRING");
        item.setEditable(1);
        when(sysConfigRepository.findByKey("site.logo")).thenReturn(Optional.of(item));

        ConfigBatchUpdateRequest request = new ConfigBatchUpdateRequest();
        ConfigUpdateItem update = new ConfigUpdateItem();
        update.setConfigKey("site.logo");
        update.setConfigValue("javascript:alert(1)");
        request.setItems(List.of(update));

        BusinessException ex = assertThrows(BusinessException.class, () -> systemConfigService.batchUpdate(request));
        assertEquals(ResultCode.CONFIG_VALUE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void create_success() {
        when(sysConfigRepository.findByKey("custom.notify.enabled")).thenReturn(Optional.empty());
        when(sysConfigRepository.insert(
                "custom.notify.enabled", "通知开关", "false", "custom",
                "BOOLEAN", "业务通知总开关", 1, 100))
                .thenReturn(42L);
        when(sysConfigRepository.findAllValues()).thenReturn(Map.of("custom.notify.enabled", "false"));

        ConfigCreateRequest request = new ConfigCreateRequest();
        request.setConfigKey("custom.notify.enabled");
        request.setConfigName("通知开关");
        request.setConfigValue("false");
        request.setConfigGroup("custom");
        request.setValueType("BOOLEAN");
        request.setDescription("业务通知总开关");
        request.setSortNo(100);

        Long id = systemConfigService.create(request);

        assertEquals(42L, id);
        verify(sysConfigRepository).insert(
                "custom.notify.enabled", "通知开关", "false", "custom",
                "BOOLEAN", "业务通知总开关", 1, 100);
    }

    @Test
    void create_duplicateKey() {
        ConfigItem existing = new ConfigItem();
        existing.setConfigKey("site.name");
        when(sysConfigRepository.findByKey("site.name")).thenReturn(Optional.of(existing));

        ConfigCreateRequest request = new ConfigCreateRequest();
        request.setConfigKey("site.name");
        request.setConfigName("站点名称");
        request.setConfigGroup("site");
        request.setValueType("STRING");

        BusinessException ex = assertThrows(BusinessException.class, () -> systemConfigService.create(request));
        assertEquals(ResultCode.CONFIG_KEY_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void updateMetadata_success() {
        ConfigItem existing = new ConfigItem();
        existing.setId(5L);
        existing.setConfigKey("custom.foo");
        existing.setEditable(1);
        when(sysConfigRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(sysConfigRepository.updateMetadata(5L, "新名称", "custom", "说明", 10)).thenReturn(1);

        ConfigMetaUpdateRequest request = new ConfigMetaUpdateRequest();
        request.setConfigName("新名称");
        request.setConfigGroup("custom");
        request.setDescription("说明");
        request.setSortNo(10);

        systemConfigService.updateMetadata(5L, request);

        verify(sysConfigRepository).updateMetadata(5L, "新名称", "custom", "说明", 10);
    }

    @Test
    void delete_builtinBlocked() {
        ConfigItem existing = new ConfigItem();
        existing.setId(1L);
        existing.setConfigKey("site.name");
        when(sysConfigRepository.findById(1L)).thenReturn(Optional.of(existing));

        BusinessException ex = assertThrows(BusinessException.class, () -> systemConfigService.delete(1L));
        assertEquals(ResultCode.CONFIG_NOT_DELETABLE.getCode(), ex.getCode());
    }

    @Test
    void delete_customSuccess() {
        ConfigItem existing = new ConfigItem();
        existing.setId(9L);
        existing.setConfigKey("custom.notify.enabled");
        when(sysConfigRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(sysConfigRepository.softDeleteById(9L)).thenReturn(1);
        when(sysConfigRepository.findAllValues()).thenReturn(Map.of());

        systemConfigService.delete(9L);

        verify(sysConfigRepository).softDeleteById(9L);
    }

    @Test
    void listConfigs_enrichesBuiltinFlags() {
        ConfigItem builtin = new ConfigItem();
        builtin.setConfigKey("site.name");
        ConfigItem custom = new ConfigItem();
        custom.setConfigKey("custom.foo");
        when(sysConfigRepository.findAll(null)).thenReturn(List.of(builtin, custom));

        List<ConfigItem> list = systemConfigService.listConfigs(null);

        assertEquals(Boolean.TRUE, list.get(0).getBuiltin());
        assertEquals(Boolean.FALSE, list.get(0).getDeletable());
        assertEquals(Boolean.TRUE, list.get(0).getWired());
        assertEquals("登录页标题、侧边栏品牌", list.get(0).getWiredScope());
        assertEquals(Boolean.FALSE, list.get(1).getWired());
        assertEquals(Boolean.TRUE, list.get(1).getDeletable());
    }

    @Test
    void getString_readsFromCache() {
        when(sysConfigRepository.findAllValues()).thenReturn(Map.of("custom.msg", "hello"));

        String value = systemConfigService.getString("custom.msg", "default");

        assertEquals("hello", value);
        assertEquals("default", systemConfigService.getString("missing.key", "default"));
    }

    @Test
    void validatePasswordLength_tooLong() {
        when(sysConfigRepository.findAllValues()).thenReturn(Map.of("auth.password.min_length", "8"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> systemConfigService.validatePasswordLength("a".repeat(65)));
        assertEquals(ResultCode.AUTH_PASSWORD_TOO_LONG.getCode(), ex.getCode());
    }

    @Test
    void getBoolean_parsesValue() {
        when(sysConfigRepository.findAllValues()).thenReturn(Map.of("custom.flag", "true"));

        assertEquals(true, systemConfigService.getBoolean("custom.flag", false));
        assertEquals(true, systemConfigService.getBoolean("missing", true));
    }
}
