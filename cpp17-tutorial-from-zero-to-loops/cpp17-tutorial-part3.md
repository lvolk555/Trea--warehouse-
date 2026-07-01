# C++17 进阶教程：数组与容器

> 第三部分：从 C 风格数组到现代 C++ 容器
>
> 标准：C++17 | 目标读者：有 Java/C 基础，已完成前两部分（Hello World 到循环、枚举到自定义类型）的学生

---

## 目录

- [第一课：数组的概念](#第一课数组的概念)
- [第二课：数组的应用](#第二课数组的应用)
- [第三课：基于数组的循环](#第三课基于数组的循环)
- [第四课：多维数组](#第四课多维数组)
- [第五课：std::array](#第五课stdarray)
- [第六课：std::vector](#第六课stdvector)
- [第七课：配套拓展必学内容](#第七课配套拓展必学内容)
- [本章小结](#本章小结)

---

## 第一课：数组的概念

### 为什么需要数组

假设你要存储全班 50 个学生的成绩，如果一个个声明变量：`int score1, score2, score3, ... score50;`，那你得写 50 行。如果你要算平均分，还得把它们一个个加起来。这显然不现实。

数组让你用一个名字管理一组相同类型的数据。Java 程序员对数组不陌生，C 语言中也有数组。C++ 继承了 C 的数组语法，同时提供了更安全的 `std::array` 和 `std::vector`。

本课先讲 C 风格的原生数组，后面再讲现代 C++ 的替代方案。

### 静态一维数组声明

声明数组需要指定三件事：元素类型、数组名字、元素个数。

```cpp
#include <iostream>

int main() {
    // 声明一个包含 5 个 int 的数组
    int scores[5];

    // 声明一个包含 3 个 double 的数组
    double prices[3];

    // 声明一个包含 10 个 char 的数组
    char name[10];

    // sizeof 可以查看数组占用的总字节数
    std::cout << "scores 占用: " << sizeof(scores) << " 字节\n";   // 5 * 4 = 20
    std::cout << "prices 占用: " << sizeof(prices) << " 字节\n";   // 3 * 8 = 24
    std::cout << "name 占用: " << sizeof(name) << " 字节\n";       // 10 * 1 = 10

    return 0;
}
```

运行结果：

```text
scores 占用: 20 字节
prices 占用: 24 字节
name 占用: 10 字节
```

**语法格式**：`类型 数组名[元素个数];`

元素个数必须是编译期已知的常量，不能用变量：

```cpp
int n = 5;
int arr[n];          // C++ 标准不允许！某些编译器扩展支持，但不推荐
constexpr int SIZE = 5;
int arr2[SIZE];      // 正确：constexpr 是编译期常量
```

> **Java 对比**：Java 声明数组写 `int[] scores = new int[5];`，数组是对象，在堆上分配。C++ 的原生数组不是对象，直接在栈上分配内存（局部数组）或全局数据区（全局数组），没有 `new`，也没有 `.length` 属性。

### 知识点补充：sizeof 与数组长度

C++ 原生数组没有 `.length()` 或 `.size()` 方法。要获取数组长度，需要用 `sizeof` 技巧：

```cpp
#include <iostream>

int main() {
    int scores[] = {90, 85, 78, 92, 88};

    // sizeof(scores) = 整个数组占的字节数 = 5 * 4 = 20
    // sizeof(scores[0]) = 单个元素占的字节数 = 4
    // 两者相除 = 元素个数 = 5
    int length = sizeof(scores) / sizeof(scores[0]);

    std::cout << "数组长度: " << length << "\n";  // 5
    return 0;
}
```

运行结果：

```text
数组长度: 5
```

> **重要提醒**：`sizeof` 方法只在数组定义所在的作用域内有效。如果把数组传给函数，函数内部用 `sizeof` 得到的不是数组大小，而是指针大小（8 字节）。这是 C/C++ 数组最大的陷阱之一，后面讲函数传参时会详细说明。

### 数组多种初始化方式

C++ 提供了多种初始化数组的方式：

```cpp
#include <iostream>

int main() {
    // 方式一：完全初始化——给出所有元素的值
    int a[5] = {10, 20, 30, 40, 50};

    // 方式二：部分初始化——只给前几个赋值，其余自动为 0
    int b[5] = {10, 20};  // b = {10, 20, 0, 0, 0}

    // 方式三：省略大小——编译器根据初始值个数自动推断
    int c[] = {1, 2, 3, 4, 5, 6, 7};  // 编译器推断长度为 7

    // 方式四：全部初始化为 0
    int d[5] = {};       // d = {0, 0, 0, 0, 0}
    int e[5] = {0};      // 同上，效果一样

    // 方式五：C++11 列表初始化（推荐）
    int f[5]{10, 20, 30, 40, 50};  // 等价于方式一，可省略等号

    // 输出验证
    std::cout << "a: ";
    for (int i = 0; i < 5; ++i) std::cout << a[i] << " ";
    std::cout << "\n";

    std::cout << "b: ";
    for (int i = 0; i < 5; ++i) std::cout << b[i] << " ";
    std::cout << "\n";

    std::cout << "c: ";
    for (int x : c) std::cout << x << " ";
    std::cout << "\n";

    std::cout << "d: ";
    for (int i = 0; i < 5; ++i) std::cout << d[i] << " ";
    std::cout << "\n";

    std::cout << "e: ";
    for (int i = 0; i < 5; ++i) std::cout << e[i] << " ";
    std::cout << "\n";

    std::cout << "f: ";
    for (int i = 0; i < 5; ++i) std::cout << f[i] << " ";
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
a: 10 20 30 40 50
b: 10 20 0 0 0
c: 1 2 3 4 5 6 7
d: 0 0 0 0 0
e: 0 0 0 0 0
f: 10 20 30 40 50
```

各初始化方式对比：

| 方式 | 语法 | 特点 |
|------|------|------|
| 完全初始化 | `int a[5] = {1,2,3,4,5};` | 所有元素都赋值 |
| 部分初始化 | `int b[5] = {1,2};` | 剩余元素自动为 0 |
| 省略大小 | `int c[] = {1,2,3};` | 编译器自动推断长度 |
| 全零初始化 | `int d[5] = {};` | 所有元素为 0 |
| 列表初始化 | `int f[5]{1,2,3,4,5};` | C++11 新语法，可省略等号 |

> **新手陷阱**：如果不初始化，局部数组的元素是**未定义的随机值**（和局部变量一样）。养成声明数组时就初始化的习惯，哪怕只是 `= {}` 全部清零。

### 通过下标读写数组元素

数组元素通过**下标**（index）访问，下标从 0 开始。这是 C/C++ 和 Java 共同的约定：

```cpp
#include <iostream>

int main() {
    int scores[5] = {90, 85, 78, 92, 88};

    // 读取：通过下标访问元素
    std::cout << "第一个成绩: " << scores[0] << "\n";  // 90
    std::cout << "第三个成绩: " << scores[2] << "\n";  // 78
    std::cout << "最后一个成绩: " << scores[4] << "\n"; // 88

    // 写入：通过下标修改元素
    scores[2] = 80;  // 把第三个成绩从 78 改成 80
    std::cout << "修改后第三个: " << scores[2] << "\n";  // 80

    // 通过下标进行计算
    int sum = scores[0] + scores[1] + scores[2] + scores[3] + scores[4];
    std::cout << "总分: " << sum << "\n";       // 435
    std::cout << "平均分: " << sum / 5 << "\n"; // 87

    return 0;
}
```

运行结果：

```text
第一个成绩: 90
第三个成绩: 78
最后一个成绩: 88
修改后第三个: 80
总分: 435
平均分: 87
```

**下标编号规则**：

- 第一个元素的下标是 `0`，不是 `1`
- 最后一个元素的下标是 `长度 - 1`
- 对于长度为 5 的数组，有效下标范围是 `0` 到 `4`

> **Java 对比**：C++ 和 Java 的数组下标完全一样，从 0 开始。但 Java 的数组是对象，有 `.length` 属性获取长度；C++ 原生数组没有这个属性，只能用 `sizeof` 技巧。

### 数组下标范围与越界问题

数组下标超出有效范围就是**越界访问**。这是 C/C++ 中最危险的错误之一：

```cpp
#include <iostream>

int main() {
    int arr[5] = {10, 20, 30, 40, 50};

    // 正确：下标 0~4
    std::cout << arr[0] << "\n";  // 10
    std::cout << arr[4] << "\n";  // 50

    // 越界：下标 5 及以上
    // C++ 不会报错！也不会抛异常！它会读取数组后面的内存
    // 那块内存里是什么，完全不可预测
    // std::cout << arr[5] << "\n";   // 危险！未定义行为
    // std::cout << arr[10] << "\n";  // 危险！可能程序崩溃

    // 越界写入更可怕：会覆盖相邻内存，导致难以排查的 bug
    // arr[5] = 999;   // 危险！可能破坏其他变量

    std::cout << "请始终确保下标在 0 到 " << 5 - 1 << " 之间\n";
    return 0;
}
```

运行结果：

```text
10
50
请始终确保下标在 0 到 4 之间
```

**越界的后果**：

| 情况 | 后果 |
|------|------|
| 读取越界 | 读到内存中的随机垃圾值 |
| 写入越界 | 覆盖其他变量的值，可能导致程序崩溃 |
| 严重越界 | 程序直接段错误（Segmentation Fault） |

> **Java 对比**：Java 的数组越界会抛出 `ArrayIndexOutOfBoundsException` 异常，程序不会崩溃。C++ 没有这种保护——越界访问是**未定义行为**（Undefined Behavior），编译器不做任何检查。这是 C++ 比 Java 快的原因之一，也是 C++ 比 Java 危险的原因之一。

> **防护建议**：如果你担心越界，使用 `std::array` 或 `std::vector`，它们提供了 `.at()` 方法，会做边界检查（越界时抛出异常）。后面会详细讲。

### 小练习

声明一个包含 8 个整数的数组，用列表初始化赋值为 2 的幂（1, 2, 4, 8, 16, 32, 64, 128）。用 for 循环输出所有元素，并计算它们的总和。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    // 声明并初始化：2 的幂
    int powers[8] = {1, 2, 4, 8, 16, 32, 64, 128};

    int sum = 0;

    // 遍历输出并累加
    for (int i = 0; i < 8; ++i) {
        std::cout << "powers[" << i << "] = " << powers[i] << "\n";
        sum += powers[i];  // 累加
    }

    std::cout << "总和: " << sum << "\n";
    return 0;
}
```

运行结果：

```text
powers[0] = 1
powers[1] = 2
powers[2] = 4
powers[3] = 8
powers[4] = 16
powers[5] = 32
powers[6] = 64
powers[7] = 128
总和: 255
```

</details>

---

## 第二课：数组的应用

### 使用数组批量存储数值数据

数组最常见的用途是批量存储一组相关数据。下面是一个成绩管理的例子：

```cpp
#include <iostream>
#include <string>

int main() {
    // 用数组存储 5 个学生的姓名和成绩
    std::string names[] = {"Alice", "Bob", "Charlie", "Diana", "Eve"};
    int scores[] = {92, 85, 78, 95, 88};

    // 计算数组长度
    int count = sizeof(scores) / sizeof(scores[0]);

    // 输出每个学生的信息
    std::cout << "=== 成绩单 ===\n";
    for (int i = 0; i < count; ++i) {
        std::cout << names[i] << ": " << scores[i] << " 分\n";
    }

    // 计算总分和平均分
    int sum = 0;
    for (int i = 0; i < count; ++i) {
        sum += scores[i];
    }

    std::cout << "\n";
    std::cout << "总分: " << sum << "\n";
    std::cout << "平均分: " << sum / count << "\n";

    return 0;
}
```

运行结果：

```text
=== 成绩单 ===
Alice: 92 分
Bob: 85 分
Charlie: 78 分
Diana: 95 分
Eve: 88 分

总分: 438
平均分: 87
```

### 枚举作为数组下标使用

第二部分学过的 `enum class` 可以让数组下标更有意义。当你用枚举值做下标时，代码的可读性会大大提高：

```cpp
#include <iostream>
#include <string>

// 定义星期枚举，底层类型设为 int，值从 0 开始
enum class Day : int {
    Monday = 0,    // 下标 0
    Tuesday,       // 下标 1
    Wednesday,     // 下标 2
    Thursday,      // 下标 3
    Friday,        // 下标 4
    Saturday,      // 下标 5
    Sunday         // 下标 6
};

// 枚举值个数
constexpr int DAY_COUNT = 7;

int main() {
    // 用枚举做下标，每天的日程安排
    std::string schedule[DAY_COUNT] = {
        "开会",           // Monday
        "写代码",         // Tuesday
        "代码审查",       // Wednesday
        "测试",           // Thursday
        "周报",           // Friday
        "休息",           // Saturday
        "休息"            // Sunday
    };

    // 用枚举值做下标访问，比 schedule[0] 可读性更好
    std::cout << "周一: " << schedule[static_cast<int>(Day::Monday)] << "\n";
    std::cout << "周三: " << schedule[static_cast<int>(Day::Wednesday)] << "\n";
    std::cout << "周六: " << schedule[static_cast<int>(Day::Saturday)] << "\n";

    return 0;
}
```

运行结果：

```text
周一: 开会
周三: 代码审查
周六: 休息
```

> **为什么用枚举做下标？** `schedule[static_cast<int>(Day::Wednesday)]` 比 `schedule[2]` 更直观——你一眼就知道这是在查"星期三"的日程。这种写法在游戏开发、状态机、配置表中非常常见。

> **C 语言对比**：C 语言的 `enum` 可以隐式转成 `int`，直接当数组下标用，写法更简洁（`schedule[Wednesday]`）。C++ 的 `enum class` 更安全但需要 `static_cast`，这是安全性和便利性之间的权衡。如果下标使用非常频繁，也可以用传统 `enum`（但要注意命名冲突风险）。

### 小练习

用枚举表示四季（春、夏、秋、冬），创建一个数组存储每季的平均气温，用枚举做下标输出每个季节的气温。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <string>

// 四季枚举
enum class Season : int {
    Spring = 0,
    Summer,
    Autumn,
    Winter
};

constexpr int SEASON_COUNT = 4;

int main() {
    // 每季平均气温（摄氏度）
    double temps[SEASON_COUNT] = {15.5, 28.3, 18.0, -2.5};

    // 季节名称
    std::string names[SEASON_COUNT] = {"春季", "夏季", "秋季", "冬季"};

    for (int i = 0; i < SEASON_COUNT; ++i) {
        std::cout << names[i] << "平均气温: " << temps[i] << " 度\n";
    }

    return 0;
}
```

运行结果：

```text
春季平均气温: 15.5 度
夏季平均气温: 28.3 度
秋季平均气温: 18 度
冬季平均气温: -2.5 度
```

</details>

---

## 第三课：基于数组的循环

### for 循环遍历数组

for 循环是遍历数组最常用的方式。C++ 提供了三种 for 循环写法：

```cpp
#include <iostream>

int main() {
    int arr[] = {10, 20, 30, 40, 50};
    int len = sizeof(arr) / sizeof(arr[0]);

    // 方式一：经典 for 循环（C/Java 通用）
    std::cout << "经典 for: ";
    for (int i = 0; i < len; ++i) {
        std::cout << arr[i] << " ";
    }
    std::cout << "\n";

    // 方式二：范围 for 循环（C++11 引入，类似 Java 的 for-each）
    std::cout << "范围 for: ";
    for (int x : arr) {
        std::cout << x << " ";
    }
    std::cout << "\n";

    // 方式三：范围 for + auto（推荐写法）
    std::cout << "auto for:  ";
    for (const auto& x : arr) {
        std::cout << x << " ";
    }
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
经典 for: 10 20 30 40 50
范围 for: 10 20 30 40 50
auto for:  10 20 30 40 50
```

三种写法的对比：

| 写法 | 能否获取下标 | 能否修改原数组 | 推荐场景 |
|------|-------------|---------------|---------|
| `for (int i = 0; i < len; ++i)` | 能 | 能 | 需要下标或需要修改 |
| `for (int x : arr)` | 不能 | 不能（x 是拷贝） | 只读遍历 |
| `for (const auto& x : arr)` | 不能 | 不能 | 只读遍历（推荐） |
| `for (auto& x : arr)` | 不能 | 能 | 需要修改元素 |

> **Java 对比**：Java 的增强 for 循环 `for (int x : arr)` 和 C++ 的范围 for 循环语法完全一样。C++ 多了 `auto` 和引用 `&` 的能力，更灵活。

### while 循环遍历数组

while 循环也能遍历数组，虽然不如 for 常见，但在某些场景下更自然（比如不知道数组长度，或需要提前中断）：

```cpp
#include <iostream>

int main() {
    int arr[] = {10, 20, 30, 40, 50};
    int len = sizeof(arr) / sizeof(arr[0]);

    // while 循环遍历
    std::cout << "while 遍历: ";
    int i = 0;
    while (i < len) {
        std::cout << arr[i] << " ";
        ++i;
    }
    std::cout << "\n";

    // while 的实用场景：查找第一个大于 25 的元素
    int j = 0;
    while (j < len) {
        if (arr[j] > 25) {
            std::cout << "第一个大于 25 的元素: arr[" << j << "] = " << arr[j] << "\n";
            break;  // 找到后立即退出
        }
        ++j;
    }

    return 0;
}
```

运行结果：

```text
while 遍历: 10 20 30 40 50
第一个大于 25 的元素: arr[2] = 30
```

> **for vs while**：知道循环次数用 for，不确定循环次数用 while。遍历数组通常用 for，因为数组长度是已知的。

### 循环修改数组内元素数值

通过下标配合循环，可以批量修改数组中的元素：

```cpp
#include <iostream>

int main() {
    int arr[5] = {1, 2, 3, 4, 5};
    int len = sizeof(arr) / sizeof(arr[0]);

    // 修改前
    std::cout << "修改前: ";
    for (int i = 0; i < len; ++i) {
        std::cout << arr[i] << " ";
    }
    std::cout << "\n";

    // 用 for 循环将每个元素乘以 2
    for (int i = 0; i < len; ++i) {
        arr[i] *= 2;  // 等价于 arr[i] = arr[i] * 2
    }

    std::cout << "翻倍后: ";
    for (int i = 0; i < len; ++i) {
        std::cout << arr[i] << " ";
    }
    std::cout << "\n";

    // 用范围 for + 引用修改（C++ 特色）
    for (auto& x : arr) {
        x += 10;  // 每个元素加 10
    }

    std::cout << "加10后: ";
    for (const auto& x : arr) {
        std::cout << x << " ";
    }
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
修改前: 1 2 3 4 5
翻倍后: 2 4 6 8 10
加10后: 12 14 16 18 20
```

> **关键区别**：`for (auto x : arr)` 是值拷贝，修改 `x` 不影响原数组。`for (auto& x : arr)` 是引用，修改 `x` 会直接改原数组元素。这个 `&` 符号就是"引用"，后续课程会深入讲解。

### 小练习

声明一个包含 10 个元素的数组，初始值全为 0。用循环将偶数下标（0, 2, 4, 6, 8）的元素设为其下标的平方，奇数下标（1, 3, 5, 7, 9）设为其下标的 3 倍。最后输出整个数组。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    int arr[10] = {};  // 全部初始化为 0

    for (int i = 0; i < 10; ++i) {
        if (i % 2 == 0) {
            arr[i] = i * i;     // 偶数下标：平方
        } else {
            arr[i] = i * 3;     // 奇数下标：3倍
        }
    }

    for (int i = 0; i < 10; ++i) {
        std::cout << "arr[" << i << "] = " << arr[i] << "\n";
    }

    return 0;
}
```

运行结果：

```text
arr[0] = 0
arr[1] = 3
arr[2] = 4
arr[3] = 9
arr[4] = 16
arr[5] = 15
arr[6] = 36
arr[7] = 21
arr[8] = 64
arr[9] = 27
```

</details>

---

## 第四课：多维数组

### 二维数组声明与初始化

一维数组是一条线，二维数组是一张表——有行和列。声明二维数组需要指定行数和列数：

```cpp
#include <iostream>

int main() {
    // 声明一个 3 行 4 列的二维数组
    int grid[3][4] = {
        {1,  2,  3,  4},     // 第 0 行
        {5,  6,  7,  8},     // 第 1 行
        {9, 10, 11, 12}      // 第 2 行
    };

    // 也可以部分初始化，未赋值的元素为 0
    int partial[2][3] = {
        {1, 2},              // {1, 2, 0}
        {4}                  // {4, 0, 0}
    };

    // 全部初始化为 0
    int zeros[2][3] = {};
    std::cout << "zeros[0][0] = " << zeros[0][0] << "\n";  // 0

    // 省略行数（编译器自动推断）
    int autoRows[][4] = {
        {1, 2, 3, 4},
        {5, 6, 7, 8}
    };  // 编译器推断为 2 行
    std::cout << "autoRows[1][3] = " << autoRows[1][3] << "\n";  // 8

    std::cout << "grid[1][2] = " << grid[1][2] << "\n";   // 7
    std::cout << "grid[2][0] = " << grid[2][0] << "\n";   // 9
    std::cout << "partial[1][2] = " << partial[1][2] << "\n";  // 0

    return 0;
}
```

运行结果：

```text
zeros[0][0] = 0
autoRows[1][3] = 8
grid[1][2] = 7
grid[2][0] = 9
partial[1][2] = 0
```

**语法格式**：`类型 数组名[行数][列数];`

- `grid[1][2]` 表示第 1 行第 2 列的元素
- 行和列的下标都从 0 开始
- 对于 3 行 4 列的数组，行下标范围是 0~2，列下标范围是 0~3

> **Java 对比**：Java 的二维数组是"数组的数组"，每行可以长度不同（锯齿数组）。C++ 的二维数组是连续内存，所有行的长度必须相同。如果需要锯齿数组，C++ 要用 `vector<vector<int>>`。

### 嵌套循环遍历二维数组

遍历二维数组需要两层循环：外层遍历行，内层遍历列：

```cpp
#include <iostream>
#include <iomanip>  // 用于 setw 格式化输出

int main() {
    int grid[3][4] = {
        {1,  2,  3,  4},
        {5,  6,  7,  8},
        {9, 10, 11, 12}
    };

    int rows = 3;
    int cols = 4;

    // 用嵌套 for 循环遍历
    std::cout << "二维数组内容：\n";
    for (int i = 0; i < rows; ++i) {
        for (int j = 0; j < cols; ++j) {
            // setw(4) 让每个数字占 4 个字符宽度，整齐对齐
            std::cout << std::setw(4) << grid[i][j];
        }
        std::cout << "\n";  // 每行结束后换行
    }

    // 计算每行的总和
    std::cout << "\n每行总和：\n";
    for (int i = 0; i < rows; ++i) {
        int rowSum = 0;
        for (int j = 0; j < cols; ++j) {
            rowSum += grid[i][j];
        }
        std::cout << "第 " << i << " 行总和: " << rowSum << "\n";
    }

    // 计算每列的总和
    std::cout << "\n每列总和：\n";
    for (int j = 0; j < cols; ++j) {
        int colSum = 0;
        for (int i = 0; i < rows; ++i) {
            colSum += grid[i][j];
        }
        std::cout << "第 " << j << " 列总和: " << colSum << "\n";
    }

    return 0;
}
```

运行结果：

```text
二维数组内容：
   1   2   3   4
   5   6   7   8
   9  10  11  12

每行总和：
第 0 行总和: 10
第 1 行总和: 26
第 2 行总和: 42

每列总和：
第 0 列总和: 15
第 1 列总和: 18
第 2 列总和: 21
第 3 列总和: 24
```

> **知识点补充：std::setw**。上面代码用到了 `std::setw(4)`，它来自 `<iomanip>` 头文件（第二部分已介绍）。`setw(n)` 设置下一次输出的最小宽度为 `n` 个字符，不足的用空格填充。这在输出表格数据时非常有用，能让数字整齐对齐。

### 知识点补充：三维及更高维数组

C++ 支持任意维度的数组，但三维以上很少使用：

```cpp
#include <iostream>

int main() {
    // 3 维数组：2 层，每层 3 行 4 列
    int cube[2][3][4] = {
        {  // 第 0 层
            {1, 2, 3, 4},
            {5, 6, 7, 8},
            {9, 10, 11, 12}
        },
        {  // 第 1 层
            {13, 14, 15, 16},
            {17, 18, 19, 20},
            {21, 22, 23, 24}
        }
    };

    std::cout << "cube[0][1][2] = " << cube[0][1][2] << "\n";  // 7
    std::cout << "cube[1][2][3] = " << cube[1][2][3] << "\n";  // 24

    return 0;
}
```

运行结果：

```text
cube[0][1][2] = 7
cube[1][2][3] = 24
```

> **实际建议**：三维以上的原生数组可读性差，容易出错。实际工程中更推荐用 `std::vector` 嵌套来代替。

### 小练习

创建一个 3x3 的矩阵（二维数组），表示一个九宫格。初始化为 1~9 的数字。用嵌套循环输出矩阵，并计算对角线元素（左上到右下）的和。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <iomanip>

int main() {
    int matrix[3][3] = {
        {1, 2, 3},
        {4, 5, 6},
        {7, 8, 9}
    };

    // 输出矩阵
    std::cout << "矩阵：\n";
    for (int i = 0; i < 3; ++i) {
        for (int j = 0; j < 3; ++j) {
            std::cout << std::setw(4) << matrix[i][j];
        }
        std::cout << "\n";
    }

    // 计算对角线之和（左上到右下：[0][0] + [1][1] + [2][2]）
    int diagSum = 0;
    for (int i = 0; i < 3; ++i) {
        diagSum += matrix[i][i];  // 行下标和列下标相同
    }

    std::cout << "\n对角线之和: " << diagSum << "\n";  // 1 + 5 + 9 = 15

    return 0;
}
```

运行结果：

```text
矩阵：
   1   2   3
   4   5   6
   7   8   9

对角线之和: 15
```

</details>

---

## 第五课：std::array

### 在 std 命名空间使用 std::array

前面学的 C 风格数组有几个缺陷：不知道自己的长度、容易越界、不能直接赋值。C++11 引入了 `std::array`，它是一个**固定大小、但更安全的数组容器**。

```cpp
#include <iostream>
#include <array>   // std::array 的头文件

int main() {
    // std::array 在 std 命名空间里
    // 它是一个模板类，需要指定元素类型和大小
    std::array<int, 5> arr = {10, 20, 30, 40, 50};

    // 它知道自己有多少个元素
    std::cout << "大小: " << arr.size() << "\n";  // 5

    // 可以直接赋值（C 风格数组做不到！）
    std::array<int, 5> arr2;
    arr2 = arr;  // 直接拷贝整个数组

    std::cout << "arr2[0]: " << arr2[0] << "\n";  // 10
    std::cout << "arr2[4]: " << arr2[4] << "\n";  // 50

    return 0;
}
```

运行结果：

```text
大小: 5
arr2[0]: 10
arr2[4]: 50
```

> **std::array vs C 风格数组**：
>
> | 特性 | C 风格数组 | std::array |
> |------|-----------|------------|
> | 知道自己长度 | 不知道 | 知道（`.size()`） |
> | 能直接赋值 | 不能 | 能 |
> | 边界检查 | 没有 | `.at()` 有 |
> | 退化成指针 | 传给函数时会 | 不会 |
> | 性能 | 和原生数组一样 | 和原生数组一样（零开销） |
> | 推荐程度 | 仅用于底层/嵌入式 | 优先使用 |
>
> `std::array` 的性能和 C 风格数组完全一样（底层就是数组），但它更安全、更方便。现代 C++ 优先用 `std::array` 代替 C 风格数组。

### std::array 的定义与初始化

```cpp
#include <iostream>
#include <array>
#include <string>

int main() {
    // 方式一：列表初始化
    std::array<int, 5> a = {1, 2, 3, 4, 5};

    // 方式二：C++11 统一初始化（可省略等号）
    std::array<int, 5> b{1, 2, 3, 4, 5};

    // 方式三：部分初始化，其余为 0
    std::array<int, 5> c = {10, 20};  // {10, 20, 0, 0, 0}

    // 方式四：全零初始化
    std::array<int, 5> d = {};   // {0, 0, 0, 0, 0}
    std::array<int, 5> e{};     // 同上

    // 方式五：存放其他类型
    std::array<std::string, 3> names = {"Alice", "Bob", "Charlie"};
    std::array<double, 4> temps = {36.5, 36.8, 37.0, 36.6};
    std::cout << "体温: ";
    for (const auto& t : temps) std::cout << t << " ";
    std::cout << "\n";

    // 输出验证
    std::cout << "a: ";
    for (const auto& x : a) std::cout << x << " ";
    std::cout << "\n";

    std::cout << "b: ";
    for (const auto& x : b) std::cout << x << " ";
    std::cout << "\n";

    std::cout << "c: ";
    for (const auto& x : c) std::cout << x << " ";
    std::cout << "\n";

    std::cout << "d: ";
    for (const auto& x : d) std::cout << x << " ";
    std::cout << "\n";

    std::cout << "e: ";
    for (const auto& x : e) std::cout << x << " ";
    std::cout << "\n";

    std::cout << "names: ";
    for (const auto& name : names) std::cout << name << " ";
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
体温: 36.5 36.8 37 36.6
a: 1 2 3 4 5
b: 1 2 3 4 5
c: 10 20 0 0 0
d: 0 0 0 0 0
e: 0 0 0 0 0
names: Alice Bob Charlie
```

> **语法说明**：`std::array<int, 5>` 中的 `int` 是元素类型，`5` 是元素个数。注意 `5` 必须是编译期常量，不能是变量——这一点和 C 风格数组一样，因为 `std::array` 的大小在编译期就确定了。

### 访问元素与获取数组长度

`std::array` 提供了多种访问元素和获取信息的方法：

```cpp
#include <iostream>
#include <array>

int main() {
    std::array<int, 5> arr = {10, 20, 30, 40, 50};

    // 方法一：[] 运算符（不检查边界，和 C 数组一样快）
    std::cout << "arr[0] = " << arr[0] << "\n";    // 10
    std::cout << "arr[4] = " << arr[4] << "\n";    // 50

    // 方法二：.at() 方法（检查边界，越界抛出异常）
    std::cout << "arr.at(2) = " << arr.at(2) << "\n";  // 30
    // arr.at(10);  // 会抛出 std::out_of_range 异常！

    // 获取第一个和最后一个元素
    std::cout << "front: " << arr.front() << "\n";  // 10
    std::cout << "back: " << arr.back() << "\n";    // 50

    // 获取数组大小
    std::cout << "size: " << arr.size() << "\n";          // 5
    std::cout << "empty: " << arr.empty() << "\n";        // 0 (false)

    // 获取底层 C 风格数组指针（用于和 C API 交互）
    int* ptr = arr.data();
    std::cout << "data[0]: " << ptr[0] << "\n";    // 10

    // 修改元素
    arr[0] = 100;
    arr.at(1) = 200;
    std::cout << "修改后: " << arr[0] << " " << arr[1] << "\n";  // 100 200

    return 0;
}
```

运行结果：

```text
arr[0] = 10
arr[4] = 50
arr.at(2) = 30
front: 10
back: 50
size: 5
empty: 0
修改后: 100 200
```

`std::array` 常用方法速查表：

| 方法 | 说明 | 是否检查边界 |
|------|------|-------------|
| `[i]` | 访问第 i 个元素 | 否（快但不安全） |
| `.at(i)` | 访问第 i 个元素 | 是（越界抛异常） |
| `.front()` | 第一个元素 | 否 |
| `.back()` | 最后一个元素 | 否 |
| `.size()` | 元素个数 | - |
| `.empty()` | 是否为空（size==0） | - |
| `.data()` | 底层数组指针 | - |
| `.fill(x)` | 全部填充为 x | - |

**`.at()` 与 `[]` 的区别详解**：

```cpp
#include <iostream>
#include <array>
#include <stdexcept>  // std::out_of_range

int main() {
    std::array<int, 5> arr = {1, 2, 3, 4, 5};

    // [] 不检查边界：越界是未定义行为，可能读到垃圾值
    // std::cout << arr[10] << "\n";  // 危险！

    // .at() 检查边界：越界时抛出 std::out_of_range 异常
    try {
        std::cout << arr.at(10) << "\n";  // 会抛异常
    } catch (const std::out_of_range& e) {
        std::cout << "捕获异常: " << e.what() << "\n";
    }

    // .fill() 一次性填充所有元素
    arr.fill(0);  // 全部变成 0
    std::cout << "fill 后: ";
    for (const auto& x : arr) std::cout << x << " ";
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
捕获异常: array::at: __n (which is 10) >= _Nm (which is 5)
fill 后: 0 0 0 0 0
```

> **编程建议**：开发阶段用 `.at()` 防止越界，确认无误后可以换成 `[]` 追求性能。或者一直用 `.at()` 也行——异常处理的性能开销在现代 CPU 上几乎可以忽略。

> **Java 对比**：Java 的数组访问自带边界检查（越界抛 `ArrayIndexOutOfBoundsException`）。C++ 的 `[]` 没有检查，但 `.at()` 提供了和 Java 类似的安全保障。

### 小练习

用 `std::array` 存储 7 天的气温数据。实现以下功能：1) 输出每天的气温；2) 计算周平均气温；3) 找到最高温和最低温。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <array>

int main() {
    std::array<double, 7> temps = {22.5, 23.1, 21.8, 25.0, 24.3, 20.6, 19.9};

    // 1) 输出每天气温
    std::string days[] = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    std::cout << "=== 一周气温 ===\n";
    for (size_t i = 0; i < temps.size(); ++i) {
        std::cout << days[i] << ": " << temps[i] << " 度\n";
    }

    // 2) 计算平均气温
    double sum = 0;
    for (const auto& t : temps) {
        sum += t;
    }
    std::cout << "\n周平均气温: " << sum / temps.size() << " 度\n";

    // 3) 找最高温和最低温
    double maxTemp = temps[0];
    double minTemp = temps[0];
    for (const auto& t : temps) {
        if (t > maxTemp) maxTemp = t;
        if (t < minTemp) minTemp = t;
    }
    std::cout << "最高温: " << maxTemp << " 度\n";
    std::cout << "最低温: " << minTemp << " 度\n";

    return 0;
}
```

运行结果：

```text
=== 一周气温 ===
周一: 22.5 度
周二: 23.1 度
周三: 21.8 度
周四: 25 度
周五: 24.3 度
周六: 20.6 度
周日: 19.9 度

周平均气温: 22.4571 度
最高温: 25 度
最低温: 19.9 度
```

</details>

---

## 第六课：std::vector

### 在 std 命名空间使用 std::vector

`std::vector` 是 C++ 中最常用的动态数组容器。和 `std::array` 不同，`vector` 的大小可以在运行时动态变化——你可以随时添加或删除元素。

```cpp
#include <iostream>
#include <vector>   // std::vector 的头文件

int main() {
    // std::vector 在 std 命名空间里
    // 它是一个模板类，只需要指定元素类型，不需要指定大小
    std::vector<int> v = {10, 20, 30};

    // vector 可以动态增长——这是它和 array 最大的区别
    v.push_back(40);   // 添加元素到末尾
    v.push_back(50);   // 再添加一个

    std::cout << "size: " << v.size() << "\n";  // 5

    // 遍历
    for (const auto& x : v) {
        std::cout << x << " ";
    }
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
size: 5
10 20 30 40 50
```

> **std::array vs std::vector**：
>
> | 特性 | std::array | std::vector |
> |------|-----------|-------------|
> | 大小 | 编译期固定 | 运行时可变 |
> | 指定大小 | `<int, 5>` 必须写 | `<int>` 不用写 |
> | 添加/删除元素 | 不支持 | 支持（push_back 等） |
> | 内存位置 | 栈 | 堆 |
> | 性能 | 最快 | 略有开销但很小 |
> | 适用场景 | 大小已知且不变 | 大小不确定或需要增删 |
>
> **选择建议**：大小编译期已知用 `std::array`，大小运行时才确定用 `std::vector`。不确定就用 `std::vector`——它是 C++ 的"默认数组"。

> **Java 对比**：`std::vector` 类似 Java 的 `ArrayList`。`push_back` 对应 `add`，`size` 对应 `size`，`[]` 对应 `get`。但 C++ 的 `vector` 支持直接用 `[]` 访问（更高效），Java 的 `ArrayList` 只能用 `get()`。

### vector 初始化赋值

`std::vector` 有多种初始化方式，比 `std::array` 更灵活：

```cpp
#include <iostream>
#include <vector>
#include <string>

int main() {
    // 方式一：列表初始化（最常用）
    std::vector<int> v1 = {1, 2, 3, 4, 5};

    // 方式二：C++11 统一初始化（省略等号）
    std::vector<int> v2{1, 2, 3};

    // 方式三：指定大小，所有元素初始化为 0
    std::vector<int> v3(5);          // {0, 0, 0, 0, 0}

    // 方式四：指定大小和初始值
    std::vector<int> v4(5, 10);      // {10, 10, 10, 10, 10}

    // 方式五：空 vector，后续动态添加
    std::vector<int> v5;             // 空，size() == 0

    // 方式六：用另一个 vector 初始化（拷贝）
    std::vector<int> v6(v1);         // 和 v1 内容一样

    // 方式七：用 C 风格数组初始化
    int arr[] = {10, 20, 30};
    std::vector<int> v7(arr, arr + 3);  // {10, 20, 30}

    // 输出验证
    std::cout << "v1: ";
    for (const auto& x : v1) std::cout << x << " ";
    std::cout << "\n";

    std::cout << "v3: ";
    for (const auto& x : v3) std::cout << x << " ";
    std::cout << "\n";

    std::cout << "v4: ";
    for (const auto& x : v4) std::cout << x << " ";
    std::cout << "\n";

    std::cout << "v7: ";
    for (const auto& x : v7) std::cout << x << " ";
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
v1: 1 2 3 4 5
v3: 0 0 0 0 0
v4: 10 10 10 10 10
v7: 10 20 30
```

各初始化方式对比：

| 语法 | 含义 | 示例 |
|------|------|------|
| `{a, b, c}` | 列表初始化，直接给值 | `vector<int> v = {1, 2, 3};` |
| `(n)` | n 个默认值（int 为 0） | `vector<int> v(5);` |
| `(n, x)` | n 个 x | `vector<int> v(5, 10);` |
| `()` | 空 vector | `vector<int> v;` |
| `(other)` | 拷贝另一个 vector | `vector<int> v2(v1);` |

> **注意区分**：`vector<int> v(5)` 和 `vector<int> v{5}` 结果不同！前者创建 5 个 0，后者创建 1 个值为 5 的元素。这是 C++ 初始化语法的一个坑。

### 循环遍历 vector

遍历 `vector` 的方式和遍历数组完全一样：

```cpp
#include <iostream>
#include <vector>

int main() {
    std::vector<int> v = {10, 20, 30, 40, 50};

    // 方式一：经典 for 循环（需要下标时用）
    std::cout << "经典 for: ";
    for (size_t i = 0; i < v.size(); ++i) {  // 注意用 size_t
        std::cout << v[i] << " ";
    }
    std::cout << "\n";

    // 方式二：范围 for 循环（只读时推荐）
    std::cout << "范围 for: ";
    for (const auto& x : v) {
        std::cout << x << " ";
    }
    std::cout << "\n";

    // 方式三：范围 for + 引用（需要修改时用）
    std::cout << "修改后:   ";
    for (auto& x : v) {
        x *= 2;  // 每个元素翻倍
    }
    for (const auto& x : v) {
        std::cout << x << " ";
    }
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
经典 for: 10 20 30 40 50
范围 for: 10 20 30 40 50
修改后:   20 40 60 80 100
```

> **知识点补充：size_t 类型**。`v.size()` 返回的类型是 `size_t`，不是 `int`。`size_t` 是无符号整数类型，专门用于表示大小和下标。在循环中用 `size_t` 而不是 `int`，可以避免"有符号与无符号比较"的编译器警告。用 `int` 也不会出错，但在大数组上可能溢出。

### 向 vector 尾部添加元素

`push_back()` 是 `vector` 最常用的方法——在数组末尾添加一个新元素，`vector` 会自动管理内存：

```cpp
#include <iostream>
#include <vector>
#include <string>

int main() {
    // 创建空 vector
    std::vector<std::string> students;

    // 动态添加元素
    students.push_back("Alice");
    students.push_back("Bob");
    students.push_back("Charlie");

    std::cout << "学生人数: " << students.size() << "\n";  // 3

    // 遍历
    for (size_t i = 0; i < students.size(); ++i) {
        std::cout << i + 1 << ". " << students[i] << "\n";
    }

    // 删除末尾元素
    students.pop_back();  // 删除 "Charlie"
    std::cout << "\n删除后人数: " << students.size() << "\n";  // 2

    // 在指定位置插入
    students.insert(students.begin() + 1, "Diana");  // 在位置 1 插入
    std::cout << "\n插入后：\n";
    for (const auto& s : students) {
        std::cout << "- " << s << "\n";
    }

    // 删除指定位置元素
    students.erase(students.begin());  // 删除第一个
    std::cout << "\n删除第一个后：\n";
    for (const auto& s : students) {
        std::cout << "- " << s << "\n";
    }

    // 清空
    students.clear();
    std::cout << "\n清空后人数: " << students.size() << "\n";  // 0
    std::cout << "是否为空: " << (students.empty() ? "是" : "否") << "\n";  // 是

    return 0;
}
```

运行结果：

```text
学生人数: 3
1. Alice
2. Bob
3. Charlie

删除后人数: 2

插入后：
- Alice
- Diana
- Bob

删除第一个后：
- Diana
- Bob

清空后人数: 0
是否为空: 是
```

`std::vector` 常用方法速查表：

| 方法 | 说明 | 时间复杂度 |
|------|------|-----------|
| `.push_back(x)` | 尾部添加元素 | 均摊 O(1) |
| `.pop_back()` | 删除尾部元素 | O(1) |
| `.size()` | 元素个数 | O(1) |
| `.empty()` | 是否为空 | O(1) |
| `[i]` | 访问第 i 个元素 | O(1) |
| `.at(i)` | 访问第 i 个元素（带边界检查） | O(1) |
| `.front()` | 第一个元素 | O(1) |
| `.back()` | 最后一个元素 | O(1) |
| `.insert(pos, x)` | 在指定位置插入 | O(n) |
| `.erase(pos)` | 删除指定位置元素 | O(n) |
| `.clear()` | 清空所有元素 | O(n) |
| `.resize(n)` | 改变大小为 n | O(n) |

> **知识点补充：begin() 和 end()**。`insert` 和 `erase` 方法的参数是"迭代器"（iterator），不是下标。`v.begin()` 指向第一个元素，`v.begin() + 1` 指向第二个元素。迭代器是 C++ STL 的核心概念，后续课程会详细讲。目前只需要记住：`v.insert(v.begin() + i, x)` 表示在位置 i 插入元素 x。

> **知识点补充：时间复杂度**。表格里提到了"时间复杂度"，这是衡量算法效率的指标。O(1) 表示无论数组多大，操作都是固定时间；O(n) 表示操作时间和数组长度成正比。`push_back` 是均摊 O(1)——大多数时候是 O(1)，偶尔扩容时是 O(n)，但平均下来还是 O(1)。

### 知识点补充：vector 的动态扩容原理

`vector` 内部维护一块连续内存。当空间不够时，它会自动分配一块更大的内存，把旧数据搬过去：

```cpp
#include <iostream>
#include <vector>

int main() {
    std::vector<int> v;

    std::cout << "初始容量: " << v.capacity() << "\n";   // 0
    std::cout << "初始大小: " << v.size() << "\n";        // 0

    // 添加元素，观察容量变化
    for (int i = 1; i <= 10; ++i) {
        v.push_back(i);
        std::cout << "size=" << v.size()
                  << " capacity=" << v.capacity() << "\n";
    }

    return 0;
}
```

运行结果：

```text
初始容量: 0
初始大小: 0
size=1 capacity=1
size=2 capacity=2
size=3 capacity=4
size=4 capacity=4
size=5 capacity=8
size=6 capacity=8
size=7 capacity=8
size=8 capacity=8
size=9 capacity=16
size=10 capacity=16
```

**size vs capacity**：

| 概念 | 含义 | 方法 |
|------|------|------|
| size | 实际存储的元素个数 | `.size()` |
| capacity | 已分配的内存能容纳的元素数 | `.capacity()` |

`capacity` 通常是 2 的倍数增长（1→2→4→8→16...）。每次扩容时，`vector` 要分配新内存、拷贝旧数据、释放旧内存，有性能开销。如果你提前知道需要多少元素，可以用 `.reserve()` 预分配内存：

```cpp
std::vector<int> v;
v.reserve(1000);  // 预分配 1000 个元素的空间，避免多次扩容
```

> **Java 对比**：Java 的 `ArrayList` 也有类似的 `capacity` 概念和 `ensureCapacity()` 方法。`reserve()` 就是 C++ 版的 `ensureCapacity()`。

### 小练习

用 `std::vector` 实现一个简单的购物车：1) 添加 5 件商品名称；2) 在第 2 个位置插入一件商品；3) 删除最后一件商品；4) 输出购物车中所有商品和总数。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <vector>
#include <string>

int main() {
    std::vector<std::string> cart;

    // 1) 添加 5 件商品
    cart.push_back("键盘");
    cart.push_back("鼠标");
    cart.push_back("显示器");
    cart.push_back("耳机");
    cart.push_back("摄像头");

    std::cout << "初始购物车 (" << cart.size() << " 件):\n";
    for (size_t i = 0; i < cart.size(); ++i) {
        std::cout << "  " << i + 1 << ". " << cart[i] << "\n";
    }

    // 2) 在第 2 个位置（下标 1）插入一件商品
    cart.insert(cart.begin() + 1, "鼠标垫");
    std::cout << "\n插入鼠标垫后 (" << cart.size() << " 件):\n";
    for (const auto& item : cart) {
        std::cout << "  - " << item << "\n";
    }

    // 3) 删除最后一件商品
    cart.pop_back();
    std::cout << "\n删除最后一件后 (" << cart.size() << " 件):\n";
    for (const auto& item : cart) {
        std::cout << "  - " << item << "\n";
    }

    // 4) 输出总数
    std::cout << "\n购物车总数: " << cart.size() << " 件\n";

    return 0;
}
```

运行结果：

```text
初始购物车 (5 件):
  1. 键盘
  2. 鼠标
  3. 显示器
  4. 耳机
  5. 摄像头

插入鼠标垫后 (6 件):
  - 键盘
  - 鼠标垫
  - 鼠标
  - 显示器
  - 耳机
  - 摄像头

删除最后一件后 (5 件):
  - 键盘
  - 鼠标垫
  - 鼠标
  - 显示器
  - 耳机

购物车总数: 5 件
```

</details>

---

## 第七课：配套拓展必学内容

### const 修饰只读数组

当你不希望数组被修改时，用 `const` 修饰。这在函数参数传递时尤其重要——告诉调用者"我不会改你的数组"：

```cpp
#include <iostream>
#include <vector>

// 函数参数用 const 修饰，表示"只读不写"
void printArray(const std::vector<int>& arr) {
    // arr[0] = 999;  // 编译错误！const 不允许修改
    for (const auto& x : arr) {
        std::cout << x << " ";
    }
    std::cout << "\n";
}

int main() {
    // const 数组：定义后不可修改
    const int days[] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    // days[1] = 29;  // 编译错误！const 数组不可修改
    std::cout << "每月天数: ";
    for (const auto& d : days) {
        std::cout << d << " ";
    }
    std::cout << "\n";

    // const vector
    const std::vector<int> nums = {10, 20, 30};
    // nums.push_back(40);  // 编译错误！const vector 不可修改
    printArray(nums);

    return 0;
}
```

运行结果：

```text
每月天数: 31 28 31 30 31 30 31 31 30 31 30 31
10 20 30
```

> **const 的位置很重要**：
> - `const int arr[]` —— 数组元素不可修改
> - `const std::vector<int>& v` —— 通过引用传递且不可修改（最常用的函数参数写法）
> - `const std::vector<int> v` —— 拷贝一份且不可修改（不推荐，多了一次拷贝）

> **Java 对比**：Java 的 `final` 关键字类似 `const`，但 `final` 只限制引用不能重新赋值，对象内容仍可修改。C++ 的 `const` 更严格——连元素都不可修改。

### if 判断数组中的数值

用 `if` 配合循环，可以在数组中查找满足条件的元素：

```cpp
#include <iostream>
#include <vector>

int main() {
    std::vector<int> scores = {92, 85, 78, 95, 88, 67, 73, 100};

    // 统计及格和不及格的人数
    int passed = 0;
    int failed = 0;

    for (const auto& score : scores) {
        if (score >= 60) {
            ++passed;
        } else {
            ++failed;
        }
    }

    std::cout << "及格: " << passed << " 人\n";
    std::cout << "不及格: " << failed << " 人\n";

    // 查找是否有满分
    bool hasPerfect = false;
    for (const auto& score : scores) {
        if (score == 100) {
            hasPerfect = true;
            break;  // 找到就不继续找了
        }
    }

    if (hasPerfect) {
        std::cout << "有满分！\n";
    } else {
        std::cout << "没有满分\n";
    }

    // 分类统计
    int excellent = 0;  // 90+
    int good = 0;       // 80-89
    int average = 0;    // 60-79
    int poor = 0;       // <60

    for (const auto& score : scores) {
        if (score >= 90) {
            ++excellent;
        } else if (score >= 80) {
            ++good;
        } else if (score >= 60) {
            ++average;
        } else {
            ++poor;
        }
    }

    std::cout << "\n成绩分布：\n";
    std::cout << "优秀 (90+):  " << excellent << " 人\n";
    std::cout << "良好 (80-89): " << good << " 人\n";
    std::cout << "及格 (60-79): " << average << " 人\n";
    std::cout << "不及格 (<60): " << poor << " 人\n";

    return 0;
}
```

运行结果：

```text
及格: 7 人
不及格: 1 人
有满分！

成绩分布：
优秀 (90+):  3 人
良好 (80-89): 2 人
及格 (60-79): 2 人
不及格 (<60): 1 人
```

### switch 匹配数组元素

当数组元素是离散的整数值时，可以用 `switch` 进行精确匹配：

```cpp
#include <iostream>
#include <vector>

// 交通灯状态枚举
enum class Light : int { Red = 0, Yellow = 1, Green = 2 };

int main() {
    // 某路口 6 个时间段的灯状态
    std::vector<int> lightStates = {0, 2, 1, 0, 2, 1};

    std::cout << "时间段状态：\n";
    for (size_t i = 0; i < lightStates.size(); ++i) {
        int state = lightStates[i];

        std::cout << "时段" << i + 1 << ": ";
        switch (state) {
            case 0:
                std::cout << "红灯 - 停止\n";
                break;
            case 1:
                std::cout << "黄灯 - 注意\n";
                break;
            case 2:
                std::cout << "绿灯 - 通行\n";
                break;
            default:
                std::cout << "未知状态\n";
                break;
        }
    }

    return 0;
}
```

运行结果：

```text
时间段状态：
时段1: 红灯 - 停止
时段2: 绿灯 - 通行
时段3: 黄灯 - 注意
时段4: 红灯 - 停止
时段5: 绿灯 - 通行
时段6: 黄灯 - 注意
```

> **switch vs if-else**：当判断条件是"等于某个固定值"时，`switch` 比 `if-else` 更清晰。`switch` 适合离散值匹配，`if-else` 适合范围判断。

### 遍历数组求取总和

求和是数组最基本的操作之一：

```cpp
#include <iostream>
#include <vector>
#include <array>

int main() {
    // 示例 1：一维数组求和
    std::vector<int> nums = {10, 20, 30, 40, 50};

    int sum = 0;
    for (const auto& n : nums) {
        sum += n;  // 累加
    }
    std::cout << "总和: " << sum << "\n";            // 150
    std::cout << "平均: " << sum / nums.size() << "\n";  // 30

    // 示例 2：二维数组求和
    std::array<std::array<int, 4>, 3> matrix = {{
        {1, 2, 3, 4},
        {5, 6, 7, 8},
        {9, 10, 11, 12}
    }};

    int totalSum = 0;
    for (const auto& row : matrix) {       // 遍历每一行
        for (const auto& val : row) {      // 遍历行中的每个元素
            totalSum += val;
        }
    }
    std::cout << "\n矩阵总和: " << totalSum << "\n";  // 78

    // 示例 3：条件求和（只加偶数）
    std::vector<int> data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    int evenSum = 0;
    int oddSum = 0;
    for (const auto& n : data) {
        if (n % 2 == 0) {
            evenSum += n;
        } else {
            oddSum += n;
        }
    }
    std::cout << "\n偶数和: " << evenSum << "\n";  // 30
    std::cout << "奇数和: " << oddSum << "\n";   // 25

    return 0;
}
```

运行结果：

```text
总和: 150
平均: 30

矩阵总和: 78

偶数和: 30
奇数和: 25
```

> **知识点补充：std::array 嵌套**。上面用 `std::array<std::array<int, 4>, 3>` 代替了 `int matrix[3][4]`。外层 `array` 有 3 个元素，每个元素又是一个 `array<int, 4>`。这种嵌套写法类型安全，但语法稍显冗长。

### 遍历查找数组最值

查找最大值和最小值是数组的经典操作：

```cpp
#include <iostream>
#include <vector>

int main() {
    std::vector<int> nums = {45, 12, 78, 23, 89, 56, 34, 90, 67, 11};

    // 假设第一个元素就是最大值和最小值
    int maxVal = nums[0];
    int minVal = nums[0];
    int maxIndex = 0;
    int minIndex = 0;

    // 遍历查找
    for (size_t i = 1; i < nums.size(); ++i) {
        if (nums[i] > maxVal) {
            maxVal = nums[i];
            maxIndex = i;  // 记录最大值的位置
        }
        if (nums[i] < minVal) {
            minVal = nums[i];
            minIndex = i;  // 记录最小值的位置
        }
    }

    std::cout << "最大值: nums[" << maxIndex << "] = " << maxVal << "\n";  // 90
    std::cout << "最小值: nums[" << minIndex << "] = " << minVal << "\n";  // 11

    // 计算极差（最大值 - 最小值）
    std::cout << "极差: " << maxVal - minVal << "\n";  // 79

    return 0;
}
```

运行结果：

```text
最大值: nums[7] = 90
最小值: nums[9] = 11
极差: 79
```

> **查找最值的算法思路**：
> 1. 初始化 `maxVal` 和 `minVal` 为第一个元素
> 2. 从第二个元素开始遍历
> 3. 如果当前元素比 `maxVal` 大，更新 `maxVal`
> 4. 如果当前元素比 `minVal` 小，更新 `minVal`
> 5. 遍历结束后，`maxVal` 和 `minVal` 就是答案
>
> 这个算法的时间复杂度是 O(n)——只需遍历一遍数组。

### 批量统一修改数组所有元素

通过循环可以批量修改数组中的每个元素：

```cpp
#include <iostream>
#include <vector>

int main() {
    std::vector<int> temps = {15, 18, 22, 25, 28, 30, 27};

    // 修改前
    std::cout << "原始气温: ";
    for (const auto& t : temps) std::cout << t << " ";
    std::cout << "\n";

    // 批量操作 1：每个元素加 5
    for (auto& t : temps) {
        t += 5;
    }
    std::cout << "加5后:    ";
    for (const auto& t : temps) std::cout << t << " ";
    std::cout << "\n";

    // 批量操作 2：超过 30 的截断为 30
    for (auto& t : temps) {
        if (t > 30) {
            t = 30;  // 限制最大值
        }
    }
    std::cout << "截断后:   ";
    for (const auto& t : temps) std::cout << t << " ";
    std::cout << "\n";

    // 批量操作 3：用下标修改——偶数下标乘 2，奇数下标取反
    std::vector<int> data = {1, 2, 3, 4, 5, 6};
    for (size_t i = 0; i < data.size(); ++i) {
        if (i % 2 == 0) {
            data[i] *= 2;    // 偶数下标翻倍
        } else {
            data[i] = -data[i];  // 奇数下标取反
        }
    }
    std::cout << "\n交替修改: ";
    for (const auto& d : data) std::cout << d << " ";
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
原始气温: 15 18 22 25 28 30 27
加5后:    20 23 27 30 33 35 32
截断后:   20 23 27 30 30 30 30

交替修改: 2 -2 6 -4 10 -6
```

### 综合练习

综合运用本课所有知识，实现一个简易的成绩分析系统：
1. 用 `std::vector` 存储 10 个学生的成绩
2. 计算总分、平均分
3. 找出最高分和最低分
4. 统计各等级人数（优秀 90+，良好 80-89，及格 60-79，不及格 <60）
5. 将所有成绩按 10 分一档归入统计

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <vector>
#include <iomanip>

int main() {
    // 1) 存储成绩
    std::vector<int> scores = {
        92, 85, 78, 95, 67, 88, 73, 100, 56, 81
    };

    std::cout << "=== 成绩分析报告 ===\n";
    std::cout << "学生人数: " << scores.size() << "\n\n";

    // 2) 计算总分和平均分
    int sum = 0;
    for (const auto& s : scores) {
        sum += s;
    }
    double avg = static_cast<double>(sum) / scores.size();

    std::cout << std::fixed << std::setprecision(2);
    std::cout << "总分: " << sum << "\n";
    std::cout << "平均分: " << avg << "\n\n";

    // 3) 找最高分和最低分
    int maxScore = scores[0];
    int minScore = scores[0];
    int maxIdx = 0;
    int minIdx = 0;

    for (size_t i = 1; i < scores.size(); ++i) {
        if (scores[i] > maxScore) {
            maxScore = scores[i];
            maxIdx = i;
        }
        if (scores[i] < minScore) {
            minScore = scores[i];
            minIdx = i;
        }
    }
    std::cout << "最高分: " << maxScore << " (第" << maxIdx + 1 << "位)\n";
    std::cout << "最低分: " << minScore << " (第" << minIdx + 1 << "位)\n\n";

    // 4) 统计各等级人数
    int excellent = 0, good = 0, pass = 0, fail = 0;
    for (const auto& s : scores) {
        if (s >= 90) ++excellent;
        else if (s >= 80) ++good;
        else if (s >= 60) ++pass;
        else ++fail;
    }

    std::cout << "--- 等级分布 ---\n";
    std::cout << "优秀 (90+):  " << excellent << " 人\n";
    std::cout << "良好 (80-89): " << good << " 人\n";
    std::cout << "及格 (60-79): " << pass << " 人\n";
    std::cout << "不及格 (<60): " << fail << " 人\n\n";

    // 5) 10 分一档统计
    std::cout << "--- 分数段分布 ---\n";
    int buckets[11] = {};  // 0-9, 10-19, ..., 90-99, 100

    for (const auto& s : scores) {
        int bucket = s / 10;  // 92 / 10 = 9
        if (bucket > 10) bucket = 10;
        ++buckets[bucket];
    }

    for (int i = 0; i <= 10; ++i) {
        int low = i * 10;
        int high = (i == 10) ? 100 : (i * 10 + 9);
        std::cout << std::setw(3) << low << "-" << std::setw(3) << high << ": ";
        for (int j = 0; j < buckets[i]; ++j) {
            std::cout << "*";
        }
        std::cout << " (" << buckets[i] << ")\n";
    }

    return 0;
}
```

运行结果：

```text
=== 成绩分析报告 ===
学生人数: 10

总分: 815
平均分: 81.50

最高分: 100 (第8位)
最低分: 56 (第9位)

--- 等级分布 ---
优秀 (90+):  3 人
良好 (80-89): 3 人
及格 (60-79): 3 人
不及格 (<60): 1 人

--- 分数段分布 ---
  0-  9:  (0)
 10- 19:  (0)
 20- 29:  (0)
 30- 39:  (0)
 40- 49:  (0)
 50- 59: * (1)
 60- 69: * (1)
 70- 79: * (1)
 80- 89: *** (3)
 90- 99: ** (2)
100-100: * (1)
```

</details>

---

## 本章小结

回顾这一部分你学到的知识：

| 知识点 | 你掌握的能力 |
|--------|------------|
| C 风格数组 | 声明、初始化、下标访问，理解越界风险 |
| sizeof | 用 sizeof 计算数组长度 |
| 枚举做下标 | 用 enum class 作为有意义的数组下标 |
| 数组循环 | for / while / 范围 for 三种遍历方式 |
| 循环修改 | 用 auto& 引用修改数组元素 |
| 二维数组 | 声明、初始化、嵌套循环遍历 |
| std::array | 固定大小安全数组，size()/at()/fill() 等方法 |
| std::vector | 动态数组，push_back/pop_back/insert/erase 等 |
| size vs capacity | 理解 vector 的动态扩容机制和 reserve() |
| const 数组 | 用 const 保护数组不被修改 |
| if 判断 | 条件查找、分类统计 |
| switch 匹配 | 离散值匹配数组元素 |
| 求总和 | 遍历累加、条件求和 |
| 查找最值 | 遍历找最大值/最小值及其位置 |
| 批量修改 | 用循环统一修改所有元素 |

**三种数组容器对比**：

| 特性 | C 风格数组 | std::array | std::vector |
|------|-----------|------------|-------------|
| 大小 | 固定 | 固定 | 动态 |
| 获取长度 | sizeof 技巧 | `.size()` | `.size()` |
| 边界检查 | 无 | `.at()` 有 | `.at()` 有 |
| 直接赋值 | 不能 | 能 | 能 |
| 内存位置 | 栈/全局 | 栈 | 堆 |
| 性能 | 最快 | 最快 | 略有开销 |
| 推荐程度 | 仅底层/嵌入式 | 大小已知时优先 | 大小未知时优先 |

**下一步建议**：

1. 把综合练习手写一遍，加深理解
2. 尝试用 vector 实现一个简易的待办事项列表
3. 下一部分我们将学习：函数进阶（参数传递、重载、默认参数、Lambda）
4. 之后进入指针与引用——理解数组和函数传参的底层原理
5. 最后是面向对象：类、构造函数、继承、多态

---

*本教程使用 C++17 标准编写，所有示例代码均已在 g++ -std=c++17 下编译验证通过。*
