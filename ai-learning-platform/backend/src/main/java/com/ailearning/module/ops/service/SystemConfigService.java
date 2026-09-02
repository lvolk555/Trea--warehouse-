package com.ailearning.module.ops.service;

import com.ailearning.common.BizException;
import com.ailearning.common.UserContext;
import com.ailearning.module.ops.entity.SystemConfig;
import com.ailearning.module.ops.mapper.SystemConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 系统设置服务：键值配置的读取与保存
 *
 * 业务方通过 isEnabled 快捷判断开关类配置；
 * 修改仅管理员可操作，未知键直接拒绝（防止脏数据）。
 *
 * AI 密钥（ai_api_key）属敏感配置：列表接口脱敏回显，保存时留空表示不修改。
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper configMapper;

    /** 敏感配置键（回显需脱敏，保存留空跳过） */
    private static final String KEY_AI_API_KEY = "ai_api_key";

    /** 已支持的配置键白名单（新增配置需在此登记） */
    private static final Map<String, String> ALLOWED_KEYS = Map.of(
            "site_name", "站点名称",
            "ai_enabled", "AI 功能总开关（1开启 0关闭）",
            "register_enabled", "开放学生自主注册（1开启 0关闭）",
            KEY_AI_API_KEY, "AI 服务 API 密钥（留空表示不修改）",
            "ai_model", "AI 模型名称（如 glm-4.7-flash）",
            "ai_base_url", "AI 服务端点（OpenAI 兼容，留空使用默认）"
    );

    /** 全量配置列表（管理端设置页展示，敏感键脱敏） */
    public List<SystemConfig> list() {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        List<SystemConfig> configs = configMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .orderByAsc(SystemConfig::getConfigKey));
        for (SystemConfig config : configs) {
            if (KEY_AI_API_KEY.equals(config.getConfigKey())) {
                config.setConfigValue(maskKey(config.getConfigValue()));
            }
        }
        return configs;
    }

    /** 批量保存配置（仅白名单内的键，逐项 upsert） */
    public List<SystemConfig> save(Map<String, String> configs) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        if (configs == null || configs.isEmpty()) {
            throw new BizException("没有需要保存的配置");
        }
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            if (!ALLOWED_KEYS.containsKey(key)) {
                throw new BizException("不支持的配置项：" + key);
            }
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (value.length() > 200) {
                throw new BizException("配置值过长：" + key);
            }
            if (key.endsWith("_enabled") && !value.equals("0") && !value.equals("1")) {
                throw new BizException("开关配置仅允许 0 或 1：" + key);
            }
            // 敏感键留空 = 不修改（避免脱敏值被原样写回）
            if (KEY_AI_API_KEY.equals(key) && value.isEmpty()) {
                continue;
            }
            upsert(key, value);
        }
        return list();
    }

    /** 按键取值（不存在返回 null），业务方使用 */
    public String getValue(String key) {
        SystemConfig config = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
                .last("LIMIT 1"));
        return config == null ? null : config.getConfigValue();
    }

    /** 按键取值，空/不存在时回退默认值 */
    public String getValueOrDefault(String key, String defaultValue) {
        String value = getValue(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /** 开关类配置是否开启（缺省视为开启，避免配置丢失导致功能意外关闭） */
    public boolean isEnabled(String key) {
        String value = getValue(key);
        return value == null || !"0".equals(value);
    }

    private void upsert(String key, String value) {
        SystemConfig existing = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
                .last("LIMIT 1"));
        if (existing == null) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setRemark(ALLOWED_KEYS.get(key));
            configMapper.insert(config);
        } else {
            existing.setConfigValue(value);
            configMapper.updateById(existing);
        }
    }

    /** 密钥脱敏：保留前 6 位与后 4 位 */
    private String maskKey(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        if (key.length() <= 10) {
            return "****";
        }
        return key.substring(0, 6) + "****" + key.substring(key.length() - 4);
    }
}
