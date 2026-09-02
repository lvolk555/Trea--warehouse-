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
import java.util.stream.Collectors;

/**
 * 系统设置服务：键值配置的读取与保存
 *
 * 业务方通过 isEnabled/isOff 快捷判断开关类配置；
 * 修改仅管理员可操作，未知键直接拒绝（防止脏数据）。
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper configMapper;

    /** 已支持的配置键白名单（新增配置需在此登记） */
    private static final Map<String, String> ALLOWED_KEYS = Map.of(
            "site_name", "站点名称",
            "ai_enabled", "AI 功能总开关（1开启 0关闭）",
            "register_enabled", "开放学生自主注册（1开启 0关闭）"
    );

    /** 全量配置列表（管理端设置页展示） */
    public List<SystemConfig> list() {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        return configMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .orderByAsc(SystemConfig::getConfigKey));
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

    /** 配置键说明（供 VO 组装） */
    public Map<String, String> keyRemarks() {
        return ALLOWED_KEYS.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
