# 后端代码模板（Spring Boot 3 + MyBatis-Plus + Sa-Token，通用型）

> 与业务领域无关。按本模板创建新模块/新接口，代码片段可直接复制后替换领域名与角色名。
> 示例统一用 `product` 领域 + `owner（资源归属者）/ admin（管理员）` 两类角色，实际项目按业务替换。

## 一、模块骨架（以 product 领域为例）

```
module/product/
├── controller/
│   ├── AdminProductController.java     # 管理员接口（/admin/product/**）
│   └── OwnerProductController.java     # 资源归属者接口（/product/**）
├── service/
│   └── ProductService.java
├── mapper/ProductMapper.java
├── entity/Product.java
└── dto/ProductSaveDTO.java
```

> Controller 按角色拆分，角色前缀按业务命名（AdminXxx / OperatorXxx / MerchantXxx / MemberXxx…）。

## 二、common 基础设施模板

### 2.1 统一响应 Result

```java
@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200; r.message = "success"; r.data = data;
        return r;
    }
    public static <T> Result<T> ok() { return ok(null); }
    public static <T> Result<T> error(String message) {
        Result<T> r = new Result<>();
        r.code = 500; r.message = message;
        return r;
    }
}
```

### 2.2 业务异常 + 全局处理

```java
public class BizException extends RuntimeException {
    private final int code;
    public BizException(String message) { super(message); this.code = 500; }
    public BizException(int code, String message) { super(message); this.code = code; }
}

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.error(e.getMessage());   // message 直接面向用户
    }
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统繁忙，请稍后重试");
    }
}
```

### 2.3 用户上下文（角色按业务定义，不硬编码领域）

```java
public class UserContext {
    // 角色编码按项目业务定义；命名与业务对齐
    public static final int ROLE_MEMBER = 1;   // 普通用户/会员（按业务改名）
    public static final int ROLE_OWNER = 2;    // 资源归属者（如商家/教师/客服主管）
    public static final int ROLE_ADMIN = 3;    // 平台管理员

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();

    public static void set(Long userId, Integer role) { USER_ID.set(userId); ROLE.set(role); }
    public static Long userId() { return USER_ID.get(); }
    public static Integer role() { return ROLE.get(); }
    public static void clear() { USER_ID.remove(); ROLE.remove(); }

    public static void checkRole(int... allowed) {
        Integer role = ROLE.get();
        if (role == null) throw new BizException(401, "未登录或登录已过期，请重新登录");
        for (int r : allowed) if (r == role) return;
        throw new BizException(403, "无权限执行该操作");
    }
}
```

## 三、Entity / Mapper / DTO 模板

```java
@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ownerId;
    private String name;
    private Integer status;        // 状态枚举注释写清含义（如 0草稿 1上架 2下架）
    private LocalDateTime createTime;
}

public interface ProductMapper extends BaseMapper<Product> {}
```

## 四、Service 模板（鉴权 / 数据权限 / 事务 / 级联 / 防N+1）

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final UserMapper userMapper;

    /** 分页列表：返回名称不返回裸 ID */
    public IPage<ProductVO> page(int page, int size, String keyword) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .like(keyword != null && !keyword.isBlank(), Product::getName, keyword)
                .orderByDesc(Product::getCreateTime);
        IPage<Product> raw = productMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量查关联名称（防 N+1）
        List<Long> ownerIds = raw.getRecords().stream()
                .map(Product::getOwnerId).distinct().toList();
        Map<Long, User> userMap = ownerIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(ownerIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        return raw.convert(p -> {
            ProductVO vo = new ProductVO();
            BeanUtils.copyProperties(p, vo);
            User u = userMap.get(p.getOwnerId());
            vo.setOwnerName(u == null ? "未知"
                    : (u.getNickname() != null && !u.getNickname().isBlank()
                            ? u.getNickname() : u.getUsername()));  // 展示名优先，回退登录名
            return vo;
        });
    }

    /** 数据权限：资源归属者只能操作自己的资源，管理员放行 */
    private void checkProductAccess(Long productId) {
        if (UserContext.role() == UserContext.ROLE_ADMIN) return;
        Product product = productMapper.selectById(productId);
        if (product == null || !product.getOwnerId().equals(UserContext.userId())) {
            throw new BizException(403, "只能管理自己创建的" + "资源");
        }
    }

    /** 级联删除：同事务清理全部关联数据，禁止孤儿记录 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long productId) {
        if (productId.equals(UserContext.userId())) {
            throw new BizException("不能删除当前登录账号");   // 账号类删除时必加
        }
        checkProductAccess(productId);
        // 按依赖顺序清理子表/关联表（评论、附件、明细、授权关系……）
        // 最后删主实体
        productMapper.deleteById(productId);
    }
}
```

## 五、Controller 模板（按角色拆分）

```java
@RestController
@RequestMapping("/admin/product")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    /** 分页列表（keyword 按名称筛选，不按 ID） */
    @GetMapping
    public Result<IPage<ProductVO>> page(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(productService.page(page, size, keyword));
    }

    @PostMapping
    public Result<Product> create(@RequestBody ProductSaveDTO dto) {
        return Result.ok(productService.create(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }
}
```

## 六、按名称关键字筛选的标准实现

```java
/** 筛选入参用 keyword 模糊匹配名称（展示名/登录名），先命中 ID 集合再过滤记录 */
public IPage<RecordVO> adminPage(int page, int size, String keyword) {
    UserContext.checkRole(UserContext.ROLE_ADMIN);
    List<Long> hitUserIds = null;
    if (keyword != null && !keyword.isBlank()) {
        String kw = keyword.trim();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .and(w -> w.like(User::getNickname, kw).or().like(User::getUsername, kw)));
        hitUserIds = users.stream().map(User::getId).toList();
        if (hitUserIds.isEmpty()) {
            return new Page<>(page, size);   // 无匹配直接返回空页
        }
    }
    IPage<Record> raw = recordMapper.selectPage(new Page<>(page, size),
            new LambdaQueryWrapper<Record>()
                    .in(hitUserIds != null, Record::getUserId, hitUserIds)
                    .orderByDesc(Record::getCreateTime));
    return raw.convert(this::toVoWithName);   // VO 填充名称
}
```

## 七、敏感配置管理（system_config，适用于密钥/第三方参数）

```java
/** 密钥回显脱敏：前 4 后 4 打码 */
private String maskKey(String key) {
    if (key == null || key.length() <= 8) return "****";
    return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
}

/** 保存：空值跳过（留空表示不修改，防止脱敏值回写覆盖真实密钥） */
@Transactional(rollbackFor = Exception.class)
public void save(Map<String, String> configs) {
    UserContext.checkRole(UserContext.ROLE_ADMIN);
    for (Map.Entry<String, String> e : configs.entrySet()) {
        if (!ALLOWED_KEYS.containsKey(e.getKey())) throw new BizException("不支持的配置项");
        String value = e.getValue();
        if (value == null || value.isBlank()) continue;   // 跳过空值
        // upsert ...
    }
}

/** 动态配置指纹缓存：改完即生效，无需重启 */
private volatile Client cachedClient;
private volatile String cachedFingerprint = "";
private Client resolveClient() {
    String apiKey = configService.getValueOrDefault("ext_api_key", defaultKey);
    String fingerprint = apiKey + "|" + baseUrl + "|" + model;
    if (cachedClient == null || !fingerprint.equals(cachedFingerprint)) {
        // 重建客户端并刷新缓存
    }
    return cachedClient;
}
```

## 八、建表 SQL 模板

```sql
CREATE TABLE `product` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `owner_id`    BIGINT       NOT NULL COMMENT '归属用户ID',
  `name`        VARCHAR(100) NOT NULL COMMENT '名称',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0草稿 1上架 2下架',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_owner_id` (`owner_id`)
) ENGINE = InnoDB COMMENT = '商品表';

-- 系统配置初始数据（键值对，通用开关/站点名/第三方接入均放这里）
INSERT INTO `system_config` (`config_key`, `config_value`, `remark`) VALUES
('site_name', '平台名称', '站点名称'),
('ext_service_enabled', '1', '外部服务总开关（1开启 0关闭）');
```

## 九、外部服务调用纪律（AI/支付/短信等任意第三方）

```java
/** 统一入口：开关 + 配置校验 */
private void checkAvailable() {
    if (!systemConfigService.isEnabled("ext_service_enabled")) {
        throw new BizException("该功能已被管理员关闭");
    }
    if (!isConfigured()) throw new BizException("服务未配置，请联系管理员");
}

/** 限流识别 + 指数退避（最多 2 次）+ 友好降级 */
try {
    return callWithRetry();
} catch (RateLimitException e) {
    throw new BizException("第三方服务限流，请稍后重试");
}
// 超时按服务特性放宽（慢服务流式 120s、非流式 150s）；
// 代理环境启用 proxyWithSystemProperties()；降级给业务提示不裸抛堆栈
```
