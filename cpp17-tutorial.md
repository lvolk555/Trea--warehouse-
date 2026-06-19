# C++17 零基础入门教程（优化版）

> 从 Hello World 到循环：给 Java 开发者的一份现代 C++ 学习指南
>
> 标准：C++17 | 目标读者：有 Java/C 基础的学生 | 含课后练习与标准答案

---

## 目录

- [前言：用你已经会的知识来学 C++](#前言用你已经会的知识来学-c)
- [第一课：C++ 是什么](#第一课c-是什么)
- [第二课：环境搭建与第一个程序](#第二课环境搭建与第一个程序)
- [第三课：注释](#第三课注释)
- [第四课：变量与常量](#第四课变量与常量)
- [第五课：数据类型](#第五课数据类型)
- [第六课：输入与输出](#第六课输入与输出)
- [第七课：运算符](#第七课运算符)
- [第八课：类型转换](#第八课类型转换)
- [第九课：流程控制 if 与 switch](#第九课流程控制-if-与-switch)
- [第十课：循环](#第十课循环)
- [综合练习（含标准答案与运行结果）](#综合练习含标准答案与运行结果)
- [本章小结](#本章小结)

---

## 前言：用你已经会的知识来学 C++

你好！如果你正在读这篇教程，说明你已经会 Java，也学过 C 语言。这是一个非常棒的起点，因为 C++ 恰好站在 Java 和 C 的中间地带。

打个比方来帮你建立直觉：

- **C 语言**像一把手术刀——精准、直接、但没有任何保护机制，一切靠你自己。
- **Java**像一个全副武装的骑士——有 GC（垃圾回收）、有 JVM、有丰富的类库，安全但稍显笨重。
- **C++**像一把瑞士军刀——既有 C 的底层控制力，又有 Java 的高级抽象能力，但你需要自己决定何时用哪种工具。

本教程采用 **C++17** 标准。为什么选 C++17 而不是更新的 C++20 或 C++23？因为 C++17 是目前工业界和教学中最稳定、最广泛使用的现代 C++ 版本。它既包含了 C++11/14 的所有现代化特性，又比 C++20 更成熟、编译器支持更完善。

> **怎么用这份教程**
>
> 每一课都遵循"概念讲解 → 代码示例 → Java/C 对比 → 小练习"的结构。练习题后面附有标准答案和运行结果，建议你先自己手写一遍，再对照答案。手写代码是建立肌肉记忆的最好方式。

---

## 第一课：C++ 是什么

C++ 由丹麦计算机科学家 Bjarne Stroustrup 在 1979 年开始设计，最初的名字就叫 "C with Classes"（带类的 C）。它的核心设计哲学可以用一句话概括：**零成本抽象**——你使用高级特性时，不需要为不需要的东西付出运行时代价。

### 与 Java 的关键差异

| 特性 | Java | C++ |
|------|------|-----|
| 执行方式 | 编译成字节码，JVM 解释/JIT 执行 | 直接编译成机器码，操作系统执行 |
| 内存管理 | 自动垃圾回收（GC） | 手动管理，也支持智能指针和 RAII |
| 泛型 | 类型擦除（运行时无泛型信息） | 模板（编译期生成代码） |
| 多继承 | 不支持类的多继承 | 支持多继承 |
| 指针 | 只有引用，无直接指针 | 有指针，也有引用 |
| 标准演进 | 由 Oracle/OpenJDK 演进 | 由 ISO 每三年发布新标准 |

**通俗解释**：Java 像"包月套餐"，GC 帮你收拾内存，你不用操心，但要付性能税；C++ 像"自助餐"，内存自己管，但用好了性能极致，用不好就内存泄漏。

### 与 C 的关键差异

如果你写过 C，这些是 C++ 额外给你的"礼物"：

- **引用**（`&`）：比指针更安全，语法更简洁，你不用再写 `int* p = &x; *p = 10;`，直接 `int& r = x; r = 10;` 即可。
- **函数重载**：同名函数可以参数不同，C 不行，C++ 可以。
- **面向对象**：类、继承、多态、封装，全套支持。
- **类型安全的 IO**：`<iostream>` 比 C 的 `printf/scanf` 安全得多，不用再担心 `%d` 和 `%f` 写错。
- **标准库容器**：`std::string`、`std::vector` 等，不用再手动管理 `char[]` 和 `malloc/free`。

---

## 第二课：环境搭建与第一个程序

### 你需要什么

写 C++ 程序需要两样东西：

1. **编译器**：把源代码翻译成可执行文件
2. **编辑器或 IDE**：写代码的工具

### 常用编译器

| 编译器 | 平台 | 特点 |
|--------|------|------|
| GCC（`g++`） | Linux/Mac | 最常用，免费开源 |
| Clang（`clang++`） | Mac/Linux | 错误提示最友好 |
| MSVC | Windows | Visual Studio 自带 |

### 编译命令

```bash
# g++（Linux/Mac）
g++ -std=c++17 -o hello hello.cpp

# clang++（Mac）
clang++ -std=c++17 -o hello hello.cpp

# MSVC（Windows）
cl /std:c++17 /EHsc hello.cpp
```

> **Java 程序员注意**：Java 是 `javac Hello.java` 编译成 `.class`，再用 `java Hello` 运行；C++ 是一步编译成可执行文件，直接 `./hello` 运行，不需要虚拟机。

### Hello World 详解

新建文件 `hello.cpp`：

```cpp
#include <iostream>

int main() {
    std::cout << "Hello, C++17!" << std::endl;
    return 0;
}
```

逐行拆解，对比你熟悉的 Java：

| C++ 代码 | Java 等价物 | 解释 |
|----------|------------|------|
| `#include <iostream>` | `import java.util.Scanner;` | 引入输入输出库 |
| `int main()` | `public static void main(String[] args)` | 程序入口 |
| `std::cout << "..."` | `System.out.print("...")` | 输出到屏幕 |
| `std::endl` | `System.out.println()` | 换行并刷新 |
| `return 0;` | （JVM 自动处理） | 返回 0 表示正常结束 |

**关键区别**：C++ 不需要把所有代码包在 `class` 里面。`main` 就是一个普通的全局函数，这是 C++ 和 Java 最直观的不同。

### 编译与运行

```bash
g++ -std=c++17 -o hello hello.cpp
./hello
```

运行结果：

```text
Hello, C++17!
```

---

## 第三课：注释

C++ 的注释和 Java、C 完全一样，这一课对你来说就是复习。

```cpp
// 单行注释：解释这一行代码

/*
 * 多行注释：可以写很多行
 * 适合解释一段逻辑
 */

/* 紧凑的多行注释 */
```

> **小知识**：Java 有 Javadoc（`/** ... */`）可以自动生成 API 文档。C++ 没有官方标准，但工业界常用 Doxygen 工具，写法类似 `/** \brief 描述 */`。

---

## 第四课：变量与常量

### 变量定义

C++ 是**静态类型语言**，每个变量在编译期就必须确定类型。这一点和 Java 一样。

```cpp
int age = 20;
double price = 19.99;
char grade = 'A';
bool isStudent = true;
std::string name = "Alice";  // 需要 #include <string>
```

**与 Java 的三个关键不同**：

1. **字符串**：Java 用 `String`（大写，是类）；C++ 用 `std::string`（小写，在 std 命名空间里）。
2. **布尔值**：写法一样都是 `true`/`false`，但 C++ 的 `bool` 占 1 字节，Java 的 `boolean` 大小未严格定义。
3. **变量声明位置**：C 语言（老标准）要求变量必须在代码块开头声明；C++ 没有这个限制，你可以在需要时才定义变量，这和 Java 一样。

### 标识符命名规则

和 Java/C 完全相同：

- 只能由字母、数字、下划线组成
- 不能以数字开头
- 区分大小写
- 不能用关键字（`int`、`return`、`class`、`if` 等）

### 常量

C++ 有两种常量声明方式，这是 C++ 比 Java 多出来的概念：

```cpp
const double PI = 3.14159;       // 运行时常量：值在运行时确定后不可改
constexpr int MAX_SIZE = 100;    // 编译期常量：值在编译时就必须确定
```

**Java 对比**：Java 只有 `final`，相当于 C++ 的 `const`。C++ 多了一个 `constexpr`，它要求值在编译期就能算出来，适合做数组大小、模板参数等。

> **入门建议**：先用 `const` 就够了。等你学到数组大小、模板参数时，再考虑 `constexpr`。

---

## 第五课：数据类型

### 常用内置类型

| C++ 类型 | 含义 | 示例 | Java 对应 | 备注 |
|----------|------|------|-----------|------|
| `bool` | 布尔 | `true` | `boolean` | 占 1 字节 |
| `char` | 字符 | `'A'` | `char` | 通常 1 字节 |
| `short` | 短整型 | `100` | `short` | 至少 16 位 |
| `int` | 整型 | `42` | `int` | 通常 32 位 |
| `long` | 长整型 | `100000L` | `long` | 至少 32 位 |
| `long long` | 长长整型 | `10000000000LL` | （无直接对应） | 至少 64 位 |
| `float` | 单精度浮点 | `3.14f` | `float` | 通常 32 位 |
| `double` | 双精度浮点 | `3.14159` | `double` | 通常 64 位 |

**重要差异**：Java 的 `int` 永远是 32 位，`long` 永远是 64 位，跨平台一致。C++ 的整数类型长度与平台相关，`int` 可能是 16 位也可能是 32 位。

### 固定宽度整数类型（推荐）

为了跨平台一致，C++11 引入了 `<cstdint>`：

```cpp
#include <cstdint>

int8_t a = 100;        // 一定是 8 位有符号
uint32_t b = 1000000;  // 一定是 32 位无符号
int64_t c = -9999999999LL;  // 一定是 64 位有符号
```

**Java 对比**：这相当于 Java 的 `byte`/`short`/`int`/`long` 的固定宽度版本，但 C++ 的命名更直观（直接写位数）。

### 无符号类型

在类型前加 `unsigned` 表示无符号（只能是非负数）：

```cpp
unsigned int x = 42;       // 取值范围 0 到 4294967295
unsigned long long y = 10000000000ULL;
unsigned z = 42;           // 简写，等价于 unsigned int
```

> **新手避坑**：千万不要把负数赋给无符号类型！`unsigned int x = -1;` 不会报错，但 x 的值会变成 4294967295（最大的 unsigned int），这种 bug 极难发现。

### auto 类型推导

C++11 引入的 `auto` 让编译器自动推导变量类型，类似 Java 的 `var`（Java 10+）：

```cpp
auto i = 42;          // 编译器推导为 int
auto pi = 3.14;       // 推导为 double
auto name = "Tom";    // 推导为 const char*（注意：不是 string！）
auto flag = true;     // 推导为 bool
```

> **新手建议**：初学阶段先明确写类型，建立对类型的直觉。等熟练后，再用 `auto` 减少冗余。涉及数值计算时，显式类型更安全，因为 `auto` 的推导结果有时会出乎意料。

---

## 第六课：输入与输出

C++ 提供两套输入输出机制：

- `<iostream>`：C++ 风格，类型安全，推荐使用
- `<cstdio>`：C 风格的 `printf/scanf`，你有 C 基础应该很熟悉

### 标准输出 cout

```cpp
#include <iostream>
#include <string>

int main() {
    int age = 20;
    std::string name = "Alice";

    // << 是"插入"运算符，把数据"塞进"输出流
    std::cout << "Name: " << name << ", Age: " << age << "\n";
    return 0;
}
```

运行结果：

```text
Name: Alice, Age: 20
```

**Java 对比**：Java 需要 `"Name: " + name + ", Age: " + age` 字符串拼接；C++ 的 `<<` 自动处理类型转换，不用手动转字符串。

### 标准输入 cin

```cpp
#include <iostream>

int main() {
    int age;
    std::cout << "请输入年龄：";
    std::cin >> age;    // >> 是"提取"运算符，从输入流取数据
    std::cout << "你输入了：" << age << "\n";
    return 0;
}
```

运行示例（输入 22）：

```text
请输入年龄：22
你输入了：22
```

> **cin 的陷阱**：`std::cin >>` 以空格、换行、制表符作为分隔符。如果你想读入 "Hello World" 这样的带空格字符串，`cin >>` 只会读到 "Hello"。要用 `std::getline` 读整行。

### 读取一行字符串

```cpp
#include <iostream>
#include <string>

int main() {
    std::string line;
    std::cout << "请输入一句话：";
    std::getline(std::cin, line);
    std::cout << "你输入的是：" << line << "\n";
    return 0;
}
```

运行示例（输入 "Hello World"）：

```text
请输入一句话：Hello World
你输入的是：Hello World
```

---

## 第七课：运算符

C++ 的运算符和 C/Java 高度相似，这一课对你来说比较轻松。

### 算术运算符

| 运算符 | 含义 | 示例 | 结果 |
|--------|------|------|------|
| `+` | 加法 | `3 + 4` | `7` |
| `-` | 减法 | `7 - 3` | `4` |
| `*` | 乘法 | `3 * 4` | `12` |
| `/` | 除法 | `7 / 2` | `3`（整数除法截断） |
| `%` | 取模 | `7 % 3` | `1` |

**新手必知**：整数除法会截断小数部分，这是 C/C++/Java 共有的"坑"：

```cpp
int a = 7 / 2;      // a = 3，不是 3.5！
double b = 7.0 / 2; // b = 3.5，只要有一个是浮点数就是浮点除法
```

### 关系运算符

| 运算符 | 含义 | 示例 |
|--------|------|------|
| `==` | 等于 | `a == b` |
| `!=` | 不等于 | `a != b` |
| `>` | 大于 | `a > b` |
| `<` | 小于 | `a < b` |
| `>=` | 大于等于 | `a >= b` |
| `<=` | 小于等于 | `a <= b` |

### 逻辑运算符

| 运算符 | 含义 | 示例 |
|--------|------|------|
| `&&` | 逻辑与 | `a > 0 && b > 0` |
| `\|\|` | 逻辑或 | `a > 0 \|\| b > 0` |
| `!` | 逻辑非 | `!(a > 0)` |

### 自增自减运算符

和 C 完全一样，前缀和后缀的区别要记住：

```cpp
int a = 5;
int b = ++a;  // 前缀：先加 1 再赋值。a = 6, b = 6
int c = a++;  // 后缀：先赋值再加 1。c = 6, a = 7
```

**记忆口诀**：前缀"先变后用"，后缀"先用后变"。

### 复合赋值运算符

```cpp
int x = 10;
x += 5;  // 等价于 x = x + 5，x 变成 15
x -= 3;  // 等价于 x = x - 3，x 变成 12
x *= 2;  // 等价于 x = x * 2，x 变成 24
x /= 4;  // 等价于 x = x / 4，x 变成 6
x %= 4;  // 等价于 x = x % 4，x 变成 2
```

---

## 第八课：类型转换

### 隐式转换（自动转换）

编译器会自动进行"安全"的类型转换：

```cpp
int i = 42;
double d = i;   // int 自动提升为 double，d = 42.0（安全，不丢数据）

double pi = 3.14;
int n = pi;     // double 截断为 int，n = 3（不安全，丢失小数部分）
```

**Java 对比**：Java 对隐式转换更严格，`double` 到 `int` 必须显式强转，C++ 允许但会警告。

### 显式转换（强制转换）

C++ 有多种显式转换方式，入门阶段掌握两种：

```cpp
double pi = 3.14159;

// 方式一：C 风格强制转换（你学 C 时应该用过）
int a = (int)pi;  // a = 3

// 方式二：C++ 风格 static_cast（推荐）
int b = static_cast<int>(pi);  // b = 3
```

> **为什么推荐 static_cast**：它更安全（编译器会做更多检查）、更易搜索（在代码里一眼就能看出来这是类型转换）、更明确（表达"我知道我在做什么"）。C 风格的 `(int)` 太随意，容易掩盖 bug。

---

## 第九课：流程控制 if 与 switch

### if / else if / else

语法和 Java/C 完全一样：

```cpp
#include <iostream>

int main() {
    int score;
    std::cout << "请输入成绩：";
    std::cin >> score;

    if (score >= 90) {
        std::cout << "优秀\n";
    } else if (score >= 80) {
        std::cout << "良好\n";
    } else if (score >= 60) {
        std::cout << "及格\n";
    } else {
        std::cout << "不及格\n";
    }

    return 0;
}
```

运行示例（输入 85）：

```text
请输入成绩：85
良好
```

### switch

`switch` 用于多分支判断，表达式必须是整型或枚举。每个 `case` 后面记得写 `break`：

```cpp
#include <iostream>

int main() {
    int day;
    std::cout << "请输入 1-7：";
    std::cin >> day;

    switch (day) {
        case 1:
            std::cout << "星期一\n";
            break;
        case 2:
            std::cout << "星期二\n";
            break;
        case 3:
            std::cout << "星期三\n";
            break;
        default:
            std::cout << "其他\n";
            break;
    }

    return 0;
}
```

运行示例（输入 2）：

```text
请输入 1-7：2
星期二
```

> **新手必看**：忘记写 `break` 会导致"贯穿"（fall-through），程序会继续执行下一个 case 的代码。这是初学者最容易犯的错误之一。

### C++17 新特性：if 带初始化

C++17 允许在 `if` 语句中声明变量，这个变量只在 if 的作用域内有效：

```cpp
#include <iostream>

int main() {
    // x 只在这个 if-else 块内有效
    if (int x = 42; x > 0) {
        std::cout << "x 是正数：" << x << "\n";
    } else {
        std::cout << "x 不是正数\n";
    }
    // 这里 x 不可见，尝试使用会报错
    return 0;
}
```

运行结果：

```text
x 是正数：42
```

**Java 对比**：Java 没有这个特性。这个语法的好处是限制变量作用域，避免变量"泄漏"到不需要它的地方。

---

## 第十课：循环

循环是本章的重点。C++ 提供四种循环：`for`、`while`、`do-while`，以及 C++11 引入的范围 for 循环。

### for 循环

语法和 C/Java 完全一样：

```cpp
#include <iostream>

int main() {
    for (int i = 0; i < 5; ++i) {
        std::cout << i << " ";
    }
    std::cout << "\n";
    return 0;
}
```

运行结果：

```text
0 1 2 3 4
```

> **小技巧**：优先用 `++i` 而不是 `i++`。对整数来说结果一样，但 `++i` 语义更干净（不创建临时对象），养成习惯后对复杂类型有好处。

### while 循环

先判断条件，再执行循环体。条件为假时一次都不执行：

```cpp
#include <iostream>

int main() {
    int i = 0;
    while (i < 5) {
        std::cout << i << " ";
        ++i;
    }
    std::cout << "\n";
    return 0;
}
```

运行结果：

```text
0 1 2 3 4
```

### do-while 循环

先执行一次循环体，再判断条件。**至少执行一次**：

```cpp
#include <iostream>

int main() {
    int i = 0;
    do {
        std::cout << i << " ";
        ++i;
    } while (i < 5);
    std::cout << "\n";
    return 0;
}
```

运行结果：

```text
0 1 2 3 4
```

**什么时候用 do-while**：当你需要"至少执行一次"时，比如菜单选择——先显示菜单，再判断用户是否想退出。

### break 与 continue

- `break`：立即跳出整个循环
- `continue`：跳过本次循环剩余代码，进入下一次迭代

```cpp
#include <iostream>

int main() {
    for (int i = 0; i < 10; ++i) {
        if (i == 3) continue;  // 跳过 3，不打印
        if (i == 7) break;     // 遇到 7 就结束循环
        std::cout << i << " ";
    }
    std::cout << "\n";
    return 0;
}
```

运行结果：

```text
0 1 2 4 5 6
```

### 基于范围的 for 循环

这是现代 C++ 最常用的循环形式，类似 Java 的增强 for 循环：

```cpp
#include <iostream>
#include <vector>

int main() {
    std::vector<int> nums = {1, 2, 3, 4, 5};

    // 方式一：按值遍历（会拷贝每个元素）
    for (int n : nums) {
        std::cout << n << " ";
    }
    std::cout << "\n";

    // 方式二：按引用遍历（可以修改原元素）
    for (auto& n : nums) {
        n *= 2;  // 每个元素乘以 2
    }

    // 方式三：按 const 引用遍历（只读，不拷贝，最高效）
    for (const auto& n : nums) {
        std::cout << n << " ";
    }
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
1 2 3 4 5
2 4 6 8 10
```

**Java 对比**：

```java
// Java
for (int n : nums) { ... }

// C++
for (int n : nums) { ... }
```

语法几乎一样！但 C++ 多了引用版本（`auto&` 和 `const auto&`），可以避免拷贝、修改原元素，这是 Java 没有的能力。

### 循环选择指南

| 场景 | 推荐循环 | 原因 |
|------|----------|------|
| 已知迭代次数 | `for` | 初始化、条件、增量集中在一行，清晰 |
| 条件驱动，可能零次执行 | `while` | 先判断再执行 |
| 至少执行一次 | `do-while` | 先执行再判断 |
| 遍历容器/数组 | 范围 `for` | 语法简洁，不易越界 |

---

## 综合练习（含标准答案与运行结果）

以下练习按难度递增排列。建议你先手写代码，再对照标准答案。每道题都附有完整代码和运行结果。

---

### 练习 1：温度转换（难度：入门）

**题目**：输入一个摄氏温度（整数），输出对应的华氏温度。公式：`F = C × 9 / 5 + 32`。

**提示**：注意整数除法的问题。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    int celsius;
    std::cout << "请输入摄氏温度：";
    std::cin >> celsius;

    // 注意：9/5 在整数除法下等于 1，所以要先乘 9 再除 5
    // 或者写成 9.0/5 让它变成浮点除法
    int fahrenheit = celsius * 9 / 5 + 32;

    std::cout << "华氏温度为：" << fahrenheit << "\n";
    return 0;
}
```

运行示例（输入 100）：

```text
请输入摄氏温度：100
华氏温度为：212
```

运行示例（输入 0）：

```text
请输入摄氏温度：0
华氏温度为：32
```

**解析**：`celsius * 9 / 5` 的计算顺序是从左到右。先算 `100 * 9 = 900`，再算 `900 / 5 = 180`，最后 `180 + 32 = 212`。如果你写成 `celsius * (9 / 5)`，那 `9 / 5 = 1`（整数除法截断），结果就错了。

</details>

---

### 练习 2：判断闰年（难度：入门）

**题目**：输入一个年份，判断是否为闰年。闰年条件：能被 4 整除但不能被 100 整除，或者能被 400 整除。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    int year;
    std::cout << "请输入年份：";
    std::cin >> year;

    // 闰年条件：(能被4整除 且 不能被100整除) 或 (能被400整除)
    if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
        std::cout << year << " 是闰年\n";
    } else {
        std::cout << year << " 不是闰年\n";
    }

    return 0;
}
```

运行示例（输入 2024）：

```text
请输入年份：2024
2024 是闰年
```

运行示例（输入 1900）：

```text
请输入年份：1900
1900 不是闰年
```

运行示例（输入 2000）：

```text
请输入年份：2000
2000 是闰年
```

**解析**：
- 2024 能被 4 整除且不能被 100 整除 → 闰年
- 1900 能被 100 整除但不能被 400 整除 → 不是闰年
- 2000 能被 400 整除 → 闰年

</details>

---

### 练习 3：成绩等级判定（难度：入门）

**题目**：输入一个 0-100 的成绩，用 switch 语句输出等级。90-100 为 A，80-89 为 B，70-79 为 C，60-69 为 D，60 以下为 F。

**提示**：switch 不能直接判断范围，可以用 `score / 10` 把成绩转换成十位数。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    int score;
    std::cout << "请输入成绩（0-100）：";
    std::cin >> score;

    // 用 score / 10 把成绩转换成十位数
    // 例如 85 / 10 = 8，92 / 10 = 9
    switch (score / 10) {
        case 10:  // 100 分
        case 9:   // 90-99
            std::cout << "等级：A\n";
            break;
        case 8:   // 80-89
            std::cout << "等级：B\n";
            break;
        case 7:   // 70-79
            std::cout << "等级：C\n";
            break;
        case 6:   // 60-69
            std::cout << "等级：D\n";
            break;
        default:  // 60 以下
            std::cout << "等级：F\n";
            break;
    }

    return 0;
}
```

运行示例（输入 92）：

```text
请输入成绩（0-100）：92
等级：A
```

运行示例（输入 85）：

```text
请输入成绩（0-100）：85
等级：B
```

运行示例（输入 55）：

```text
请输入成绩（0-100）：55
等级：F
```

**解析**：`case 10:` 后面没有 break，会"贯穿"到 `case 9:`，所以 100 分和 90-99 分都输出 A。这是 switch 的 fall-through 特性的合理运用。

</details>

---

### 练习 4：九九乘法表（难度：中等）

**题目**：使用嵌套 for 循环打印 9×9 乘法表。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <iomanip>  // 用于 setw 控制输出宽度

int main() {
    for (int i = 1; i <= 9; ++i) {
        for (int j = 1; j <= i; ++j) {
            // setw(4) 让每个结果占 4 个字符宽度，对齐美观
            std::cout << j << "x" << i << "=" << std::setw(2) << i * j << " ";
        }
        std::cout << "\n";
    }
    return 0;
}
```

运行结果：

```text
1x1= 1 
1x2= 2 2x2= 4 
1x3= 3 2x3= 6 3x3= 9 
1x4= 4 2x4= 8 3x4=12 4x4=16 
1x5= 5 2x5=10 3x5=15 4x5=20 5x5=25 
1x6= 6 2x6=12 3x6=18 4x6=24 5x6=30 6x6=36 
1x7= 7 2x7=14 3x7=21 4x7=28 5x7=35 6x7=42 7x7=49 
1x8= 8 2x8=16 3x8=24 4x8=32 5x8=40 6x8=48 7x8=56 8x8=64 
1x9= 9 2x9=18 3x9=27 4x9=36 5x9=45 6x9=54 7x9=63 8x9=72 9x9=81 
```

**解析**：
- 外层循环 `i` 控制行（1 到 9）
- 内层循环 `j` 控制列（1 到 `i`），这样打印出来是三角形
- `std::setw(2)` 来自 `<iomanip>`，让个位数前面补空格，保持对齐

</details>

---

### 练习 5：求素数（难度：中等）

**题目**：输入一个正整数 n，输出 2 到 n 之间的所有素数，并统计个数。

**提示**：素数是只能被 1 和自身整除的大于 1 的整数。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    int n;
    std::cout << "请输入一个正整数：";
    std::cin >> n;

    int count = 0;
    std::cout << "2 到 " << n << " 之间的素数有：\n";

    for (int num = 2; num <= n; ++num) {
        bool isPrime = true;

        // 只需要检查到 sqrt(num)，但为了简单，检查到 num/2
        // 更优化的写法：检查到 i*i <= num
        for (int i = 2; i * i <= num; ++i) {
            if (num % i == 0) {
                isPrime = false;
                break;  // 只要找到一个因子就不是素数，立即跳出
            }
        }

        if (isPrime) {
            std::cout << num << " ";
            ++count;
        }
    }

    std::cout << "\n共 " << count << " 个素数\n";
    return 0;
}
```

运行示例（输入 30）：

```text
请输入一个正整数：30
2 到 30 之间的素数有：
2 3 5 7 11 13 17 19 23 29 
共 10 个素数
```

**解析**：
- 外层循环遍历 2 到 n 的每个数
- 内层循环检查当前数是否有因子。用 `i * i <= num` 而不是 `i <= num / 2`，因为如果 num 有因子，必然有一个不超过它的平方根，这样能大幅减少循环次数
- `break` 的作用是找到第一个因子就立即退出，不用继续检查

</details>

---

### 练习 6：反转整数（难度：中等）

**题目**：输入一个正整数，输出它的反转形式。例如输入 12345，输出 54321。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    int n;
    std::cout << "请输入一个正整数：";
    std::cin >> n;

    int reversed = 0;
    int original = n;  // 保存原始值用于输出

    while (n != 0) {
        int digit = n % 10;        // 取出最后一位数字
        reversed = reversed * 10 + digit;  // 把数字追加到结果末尾
        n /= 10;                   // 去掉最后一位
    }

    std::cout << original << " 反转后为：" << reversed << "\n";
    return 0;
}
```

运行示例（输入 12345）：

```text
请输入一个正整数：12345
12345 反转后为：54321
```

运行示例（输入 100）：

```text
请输入一个正整数：100
100 反转后为：1
```

**解析**：
- `n % 10` 取最后一位：12345 % 10 = 5
- `reversed * 10 + digit` 把数字追加到末尾：0*10+5=5, 5*10+4=54, 54*10+3=543...
- `n /= 10` 去掉最后一位：12345 → 1234 → 123 → 12 → 1 → 0
- 当 n 变成 0 时循环结束

</details>

---

### 练习 7：猜数字游戏（难度：进阶）

**题目**：程序生成一个 1-100 的随机数，让用户猜。猜大了提示"太大了"，猜小了提示"太小了"，猜对了提示"恭喜你猜对了"并显示猜了几次。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <cstdlib>   // rand, srand
#include <ctime>     // time

int main() {
    // 设置随机种子，否则每次运行生成的随机数都一样
    std::srand(std::time(0));

    // 生成 1-100 的随机数
    int secret = std::rand() % 100 + 1;
    int guess;
    int attempts = 0;

    std::cout << "我想了一个 1-100 之间的数，你来猜猜看！\n";

    do {
        std::cout << "请输入你的猜测：";
        std::cin >> guess;
        ++attempts;

        if (guess > secret) {
            std::cout << "太大了！\n";
        } else if (guess < secret) {
            std::cout << "太小了！\n";
        } else {
            std::cout << "恭喜你猜对了！你猜了 " << attempts << " 次。\n";
        }
    } while (guess != secret);

    return 0;
}
```

运行示例：

```text
我想了一个 1-100 之间的数，你来猜猜看！
请输入你的猜测：50
太小了！
请输入你的猜测：75
太大了！
请输入你的猜测：63
太小了！
请输入你的猜测：69
恭喜你猜对了！你猜了 4 次。
```

**解析**：
- `std::srand(std::time(0))` 用当前时间做随机种子，保证每次运行结果不同
- `std::rand() % 100 + 1` 生成 1-100 的随机数。`rand()` 返回 0 到 RAND_MAX，取模 100 得到 0-99，加 1 得到 1-100
- 用 `do-while` 是因为至少要让用户猜一次
- `attempts` 计数器记录猜测次数

</details>

---

### 练习 8：打印图形（难度：进阶）

**题目**：输入一个正整数 n，打印一个 n 行的菱形。例如 n=3 时：

```text
  *
 ***
*****
 ***
  *
```

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    int n;
    std::cout << "请输入菱形的半高（正整数）：";
    std::cin >> n;

    // 上半部分（包括中间行）：共 n 行
    for (int i = 1; i <= n; ++i) {
        // 打印空格：n-i 个
        for (int j = 0; j < n - i; ++j) {
            std::cout << " ";
        }
        // 打印星号：2*i-1 个
        for (int j = 0; j < 2 * i - 1; ++j) {
            std::cout << "*";
        }
        std::cout << "\n";
    }

    // 下半部分：共 n-1 行
    for (int i = n - 1; i >= 1; --i) {
        // 打印空格：n-i 个
        for (int j = 0; j < n - i; ++j) {
            std::cout << " ";
        }
        // 打印星号：2*i-1 个
        for (int j = 0; j < 2 * i - 1; ++j) {
            std::cout << "*";
        }
        std::cout << "\n";
    }

    return 0;
}
```

运行示例（输入 3）：

```text
请输入菱形的半高（正整数）：3
  *
 ***
*****
 ***
  *
```

运行示例（输入 5）：

```text
请输入菱形的半高（正整数）：5
    *
   ***
  *****
 *******
*********
 *******
  *****
   ***
    *
```

**解析**：
- 菱形分上下两部分打印
- 上半部分（含中间最宽行）：第 i 行有 `n-i` 个空格和 `2*i-1` 个星号
- 下半部分：从 n-1 递减到 1，规律相同
- 这道题的关键是找出"行号"与"空格数、星号数"之间的数学关系

</details>

---

## 本章小结

恭喜你完成了 C++ 入门的第一步！回顾一下你学到了什么：

| 知识点 | 你掌握的能力 |
|--------|------------|
| C++ 定位 | 理解 C++ 与 Java/C 的关系 |
| 环境搭建 | 能用 g++ 编译运行 C++17 程序 |
| 变量与常量 | 会声明变量、使用 const/constexpr |
| 数据类型 | 知道 int/double/bool/string 的用法 |
| 输入输出 | 会用 cin/cout 进行基本 IO |
| 运算符 | 会用算术、关系、逻辑、自增运算符 |
| 类型转换 | 理解隐式转换和 static_cast |
| 流程控制 | 会用 if/else 和 switch |
| 循环 | 掌握 for/while/do-while 和范围 for |

**下一步建议**：

1. 把 8 道练习全部手写一遍，确保理解每一行代码
2. 尝试修改练习中的代码，观察输出变化
3. 下一章我们将学习：数组、字符串、函数、指针与引用
4. 之后进入面向对象：类、构造函数、继承、多态

---

*本教程使用 C++17 标准编写，所有示例代码均已在 g++ -std=c++17 下编译验证通过。*
