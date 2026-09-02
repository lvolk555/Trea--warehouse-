# 后端代码模板（Spring Boot 3 + MyBatis-Plus + Sa-Token）

> 按本模板创建新模块/新接口。所有代码片段可直接复制后替换领域名。

## 一、模块骨架（以 course 领域为例）

```
module/course/
├── controller/
│   ├── AdminCourseController.java      # 管理员接口（/admin/course/**）
│   ├── TeacherCourseController.java     # 教师接口（/teacher/course/**）
│   └── StudentCourseController.java     # 学生接口（/course/**）
├── service/
│   ├── CourseService.java
│   └── CourseStudentService.java
├── mapper/CourseMapper.java
├── entity/Course.java
└── dto/CourseSaveDTO.java
```

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

### 2.3 用户上下文

```java
public class UserContext {
    public static final int ROLE_STUDENT = 1;
    public static final int ROLE_TEACHER = 2;
    public static final int ROLE_ADMIN = 3;

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
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teacherId;
    private String title;
    private Integer status;        // 0 待审核 1 上架 2 下架（注释写清含义）
    private LocalDateTime createTime;
}

public interface CourseMapper extends BaseMapper<Course> {}
```

## 四、Service 模板（含鉴权 / 数据权限 / 事务 / 级联）

```java
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    /** 分页列表：返回名称不返回裸 ID */
    public IPage<CourseVO> page(int page, int size, String keyword) {
        UserContext.checkRole(UserContext.ROLE_ADMIN);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .like(keyword != null && !keyword.isBlank(), Course::getTitle, keyword)
                .orderByDesc(Course::getCreateTime);
        IPage<Course> raw = courseMapper.selectPage(new Page<>(page, size), wrapper);

        // 批量查关联名称（防 N+1）
        List<Long> teacherIds = raw.getRecords().stream()
                .map(Course::getTeacherId).distinct().toList();
        Map<Long, User> userMap = teacherIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(teacherIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        return raw.convert(c -> {
            CourseVO vo = new CourseVO();
            BeanUtils.copyProperties(c, vo);
            User u = userMap.get(c.getTeacherId());
            vo.setTeacherName(u == null ? "未知教师"
                    : (u.getNickname() != null && !u.getNickname().isBlank()
                            ? u.getNickname() : u.getUsername()));  // 昵称优先
            return vo;
        });
    }

    /** 数据权限：教师只能操作自己的资源，管理员放行 */
    private void checkCourseAccess(Long courseId) {
        if (UserContext.role() == UserContext.ROLE_ADMIN) return;
        Course course = courseMapper.selectById(courseId);
        if (course == null || !course.getTeacherId().equals(UserContext.userId())) {
            throw new BizException(403, "只能管理自己创建的课程");
        }
    }

    /** 级联删除：同事务清理全部关联数据 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long courseId) {
        checkCourseAccess(courseId);
        // 按依赖顺序清理：章节/小节 → 题目 → 试卷 → 选课 → 学习记录 → ...
        // 最后删主实体
        courseMapper.deleteById(courseId);
    }
}
```

## 五、Controller 模板（按角色拆分）

```java
@RestController
@RequestMapping("/admin/course")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;

    /** 分页列表（keyword 按名称筛选，不按 ID） */
    @GetMapping
    public Result<IPage<CourseVO>> page(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(courseService.page(page, size, keyword));
    }

    @PostMapping
    public Result<Course> create(@RequestBody CourseSaveDTO dto) {
        return Result.ok(courseService.create(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.ok();
    }
}
```

## 六、按名称关键字筛选的标准实现

```java
/** 筛选入参用 keyword 模糊匹配名称（昵称/用户名），先命中 ID 集合再过滤记录 */
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

## 七、敏感配置管理（system_config）

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
    String apiKey = configService.getValueOrDefault("ai_api_key", defaultKey);
    String fingerprint = apiKey + "|" + baseUrl + "|" + model;
    if (cachedClient == null || !fingerprint.equals(cachedFingerprint)) {
        // 重建客户端并刷新缓存
    }
    return cachedClient;
}
```

## 八、建表 SQL 模板

```sql
CREATE TABLE `course` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `teacher_id`  BIGINT       NOT NULL COMMENT '教师ID',
  `title`       VARCHAR(100) NOT NULL COMMENT '课程标题',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待审核 1上架 2下架',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`)
) ENGINE = InnoDB COMMENT = '课程表';

-- 系统配置初始数据（键值对）
INSERT INTO `system_config` (`config_key`, `config_value`, `remark`) VALUES
('site_name', '平台名称', '站点名称'),
('ai_enabled', '1', 'AI 功能总开关（1开启 0关闭）');
```

## 九、外部服务调用纪律（AI 等场景）

```java
/** 统一入口：开关 + 配置校验 */
private void checkAvailable() {
    if (!systemConfigService.isEnabled("ai_enabled")) {
        throw new BizException("AI 功能已被管理员关闭");
    }
    if (!isConfigured()) throw new BizException("AI 服务未配置，请联系管理员");
}

/** 429 识别 + 指数退避（最多 2 次）+ 友好降级 */
try {
    return callWithRetry();
} catch (RateLimitException e) {
    throw new BizException("AI 服务限流，请稍后重试");
}
// 超时：流式 120s、非流式 150s；代理环境启用 proxyWithSystemProperties()
```
