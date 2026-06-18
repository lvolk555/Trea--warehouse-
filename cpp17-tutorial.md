# C++17 零基础入门教程

> 从 Hello World 到循环：给 Java 开发者的一份现代 C++ 学习指南
>
> 标准：C++17 | 目标读者：有 Java/C 基础的学生 | 含课后练习

---

## 目录

- [前言：Java 与 C 背景如何理解 C++](#前言java-与-c-背景如何理解-c)
- [C++ 是什么](#c-是什么)
- [环境搭建与第一个程序](#环境搭建与第一个程序)
- [注释](#注释)
- [变量与常量](#变量与常量)
- [数据类型](#数据类型)
- [输入与输出](#输入与输出)
- [运算符](#运算符)
- [类型转换](#类型转换)
- [流程控制：if / switch](#流程控制if--switch)
- [循环：for / while / do-while / range-based for](#循环for--while--do-while--range-based-for)
- [综合练习](#综合练习)
- [本章小结](#本章小结)

---

## 前言：Java 与 C 背景如何理解 C++

你已经学过 Java，也学过 C，这让你拥有一个非常理想的起点。Java 让你熟悉类、对象、引用、垃圾回收和跨平台字节码；C 让你理解指针、内存、编译和直接操作机器。C++ 恰好站在两者之间：**它既是一门“C 的超集”，又是一门支持面向对象、泛型编程、函数式特性的多范式语言**。

本教程采用 **C++17** 标准。C++17 是 2017 年发布的 ISO 标准，也是目前工业界和教学中最常用、最稳定的现代 C++ 版本之一。它增加了 `std::optional`、结构化绑定、`if constexpr`、`std::string_view` 等特性，不过这些进阶内容会在后续教程中展开。本章只讲到循环，目标是帮你建立 C++ 的语法直觉。

> **学习方法建议**
>
> 每看完一个小节，把示例代码亲手敲一遍并编译运行。C++ 是编译型语言，只有在“写代码 → 编译 → 运行 → 看报错 → 修正”的循环中，你才能真正掌握它。

---

## C++ 是什么

C++ 由 Bjarne Stroustrup 在 1979 年开始设计，最初叫“C with Classes”。它的核心设计哲学是：**为程序员提供零成本抽象（zero-overhead abstraction）**——也就是说，你使用高级特性时，不需要为不需要的东西付出代价。

### 与 Java 的关键差异

| 特性 | Java | C++ |
|------|------|-----|
| 执行方式 | 编译成字节码，JVM 解释/ JIT 执行 | 编译成机器码，操作系统直接执行 |
| 内存管理 | 自动垃圾回收（GC） | 手动管理，也支持智能指针和 RAII |
| 泛型 | 类型擦除（运行时无泛型信息） | 模板（编译期生成代码） |
| 多继承 | 不支持类的多继承 | 支持多继承 |
| 指针 | 只有引用，无直接指针 | 有指针，也有引用 |
| 标准 | 由 Oracle/OpenJDK 演进 | 由 ISO 每三年发布新标准（C++11/14/17/20/23） |

### 与 C 的关键差异

- C++ 支持**引用**（`&`），比指针更安全、语法更简洁。
- C++ 有**函数重载**：同名函数可以参数不同。
- C++ 支持面向对象：类、继承、多态、封装。
- C++ 的 `<iostream>` 提供类型安全的输入输出。
- C++ 标准库提供 `std::string`、`std::vector` 等容器，不需要手动管理字符串和数组。

---

## 环境搭建与第一个程序

写 C++ 程序，你需要两样东西：**编译器**和**文本编辑器/IDE**。编译器把源代码翻译成可执行文件，IDE 帮你编辑、调试、运行。

### 常用编译器

- **GCC**（`g++`）：Linux 最常用，免费开源。
- **Clang**（`clang++`）：Mac 和 Linux 常用，错误提示更友好。
- **MSVC**：Windows 上 Visual Studio 自带的编译器。

### 推荐使用 C++17 的编译命令

```bash
# g++
g++ -std=c++17 -o hello hello.cpp

# clang++
clang++ -std=c++17 -o hello hello.cpp

# MSVC
cl /std:c++17 /EHsc hello.cpp
```

### Hello World

新建文件 `hello.cpp`，输入以下内容：

```cpp
#include <iostream>

int main() {
    std::cout << "Hello, C++17!" << std::endl;
    return 0;
}
```

逐行解释：

- `#include <iostream>`：引入输入输出流库，相当于 Java 的 `System.in` / `System.out`。
- `int main()`：程序入口函数，必须返回 `int`。
- `std::cout`：标准输出流对象，`<<` 是“插入”运算符，把数据输出到屏幕。
- `std::endl`：输出换行并刷新缓冲区；也可以写成 `'\n'`。
- `return 0;`：向操作系统返回 0，表示程序正常结束。

> **Java 程序员注意**
>
> C++ 没有 `public class Main` 这样的入口类要求。程序入口就是一个名为 `main` 的全局函数。

### 编译与运行

```bash
g++ -std=c++17 -o hello hello.cpp
./hello
```

如果编译成功，终端会输出：

```text
Hello, C++17!
```

---

## 注释

C++ 的注释与 Java 完全相同，分为单行注释和多行注释。

```cpp
// 这是单行注释

/*
 * 这是多行注释
 * 可以写很多行
 */

/* 这也是多行注释，但更紧凑 */
```

C++ 还有一个不太常用的文档注释风格 `/** ... */`，但不像 Java 有 Javadoc 标准。工业界常用 Doxygen 工具生成文档。

---

## 变量与常量

C++ 是**静态类型语言**，每个变量在编译期就必须确定类型。这一点与 Java 相同，与 Python/JavaScript 不同。

### 变量定义

```cpp
int age = 20;
double price = 19.99;
char grade = 'A';
bool isStudent = true;
std::string name = "Alice";
```

与 Java 不同的是：

- C++ 的 `bool` 值是 `true` / `false`，不是 Java 的 `true` / `false` 字面值（虽然写法一样）。
- 字符串类型是 `std::string`，需要 `#include <string>`。
- C++ 支持在需要时才定义变量，而不像旧版 C 要求变量声明必须放在函数开头。

### 标识符命名规则

- 只能由字母、数字、下划线组成。
- 不能以数字开头。
- 区分大小写。
- 不能使用关键字，如 `int`、`return`、`class`、`if` 等。

### 常量

C++ 有两种声明常量的方式：`const` 和 `constexpr`。

```cpp
const double PI = 3.14159;       // 运行时常量
constexpr int MAX_SIZE = 100;    // 编译期常量

int main() {
    const int local = 42;
    // local = 43;  // 错误：不能修改常量
    return 0;
}
```

> **const vs constexpr**
>
> `const` 表示“只读”，值可以在运行时才确定；`constexpr` 表示“编译期就能确定”，常用于数组大小、模板参数等场景。入门阶段，优先使用 `const`。

---

## 数据类型

C++ 内置了丰富的数据类型。与 Java 相比，C++ 的整数类型长度与平台相关，但更现代的做法是使用 `<cstdint>` 中固定宽度的类型。

### 常用内置类型

| C++ 类型 | 含义 | 示例值 | 备注 |
|----------|------|--------|------|
| `bool` | 布尔 | `true`, `false` | 占 1 字节 |
| `char` | 字符 | `'A'` | 通常 1 字节 |
| `short` | 短整型 | `100` | 至少 16 位 |
| `int` | 整型 | `42` | 通常 32 位 |
| `long` | 长整型 | `100000L` | 至少 32 位 |
| `long long` | 长长整型 | `10000000000LL` | 至少 64 位 |
| `float` | 单精度浮点 | `3.14f` | 通常 32 位 |
| `double` | 双精度浮点 | `3.14159` | 通常 64 位 |

### 固定宽度整数类型（推荐）

C++11 起引入 `<cstdint>`，可以写出跨平台一致的整型：

```cpp
#include <cstdint>

int8_t a = 100;       // 8 位有符号整数
uint32_t b = 1000000; // 32 位无符号整数
int64_t c = -9999999999LL;
```

### 无符号类型

在类型前加 `unsigned` 表示无符号：

```cpp
unsigned int x = 42;
unsigned long long y = 10000000000ULL;

// 通常简写
unsigned z = 42;
```

> **注意**
>
> 不要把负数赋给无符号类型，否则会发生大整数回绕。例如 `unsigned int x = -1;` 会得到一个非常大的正数。

### auto 类型推导

C++11 引入的 `auto` 让编译器自动推导变量类型。在 C++17 中，`auto` 的使用更加成熟和推荐。

```cpp
auto i = 42;          // int
auto pi = 3.14;       // double
auto name = "Tom";    // const char*
auto flag = true;     // bool

for (auto i = 0; i < 10; ++i) {
    std::cout << i << " ";
}
```

> **建议**
>
> 初学时，先明确写类型建立直觉；熟练后，可以用 `auto` 减少冗余。但涉及数值计算时，显式类型更安全。

---

## 输入与输出

C++ 提供两套输入输出机制：`<iostream>`（C++ 风格）和 `<cstdio>`（C 风格）。推荐初学者使用 `<iostream>`，因为它类型安全、可扩展。

### 标准输出 cout

```cpp
#include <iostream>
#include <string>

int main() {
    int age = 20;
    std::string name = "Alice";

    std::cout << "Name: " << name << ", Age: " << age << "\n";
    return 0;
}
```

### 标准输入 cin

```cpp
#include <iostream>

int main() {
    int age;
    std::cout << "请输入年龄：";
    std::cin >> age;
    std::cout << "你输入了：" << age << "\n";
    return 0;
}
```

> **cin 的陷阱**
>
> `std::cin >>` 以空白字符（空格、换行、制表符）作为分隔符。如果要用 `cin` 读取一整行包含空格的字符串，应该使用 `std::getline(std::cin, str)`。

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

### C 风格输入输出（可选）

因为你有 C 基础，这里也列出 C 风格的用法，但建议新项目优先用 C++ 风格：

```cpp
#include <cstdio>

int main() {
    int a;
    printf("请输入一个整数：");
    scanf("%d", &a);
    printf("你输入了 %d\n", a);
    return 0;
}
```

> **与 Java 对比**
>
> Java 中 `System.out.println("Age: " + age)` 需要字符串拼接；C++ 中 `std::cout << "Age: " << age` 自动处理类型，不需要手动转字符串。

---

## 运算符

C++ 的运算符与 C/Java 高度相似。下面按类别列出最常用的部分。

### 算术运算符

| 运算符 | 含义 | 示例 |
|--------|------|------|
| `+` | 加法 | `a + b` |
| `-` | 减法 | `a - b` |
| `*` | 乘法 | `a * b` |
| `/` | 除法 | `a / b` |
| `%` | 取模 | `a % b` |

整数除法会截断小数：

```cpp
int a = 7 / 2;   // a = 3，不是 3.5
double b = 7.0 / 2; // b = 3.5
```

### 关系运算符

| 运算符 | 含义 |
|--------|------|
| `==` | 等于 |
| `!=` | 不等于 |
| `>` | 大于 |
| `<` | 小于 |
| `>=` | 大于等于 |
| `<=` | 小于等于 |

### 逻辑运算符

| 运算符 | 含义 | 示例 |
|--------|------|------|
| `&&` | 逻辑与 | `a > 0 && b > 0` |
| `\|\|` | 逻辑或 | `a > 0 \|\| b > 0` |
| `!` | 逻辑非 | `!(a > 0)` |

### 自增自减运算符

C++ 完全继承了 C 的前缀和后缀自增：

```cpp
int a = 5;
int b = ++a; // 先加 1，再赋值：a = 6, b = 6
int c = a++; // 先赋值，再加 1：c = 6, a = 7
```

### 复合赋值运算符

```cpp
int x = 10;
x += 5;  // x = x + 5，x 变为 15
x -= 3;  // x = x - 3，x 变为 12
x *= 2;  // x = x * 2，x 变为 24
x /= 4;  // x = x / 4，x 变为 6
```

---

## 类型转换

C++ 支持隐式转换和显式转换。由于 C++ 是强类型语言，类型转换需要特别小心。

### 隐式转换

```cpp
int i = 42;
double d = i; // int 自动提升为 double，d = 42.0

double pi = 3.14;
int n = pi;   // double 截断为 int，n = 3（丢失小数）
```

### 显式转换

C++ 提供多种显式转换方式，入门阶段先掌握 C 风格强制转换和 C++ 风格的 `static_cast`。

```cpp
double pi = 3.14159;

// C 风格
int a = (int)pi;

// C++ 风格（推荐）
int b = static_cast<int>(pi);

std::cout << "a = " << a << ", b = " << b << "\n";
```

> **建议**
>
> 优先使用 `static_cast`，因为它更安全、可读性更好，编译器也能做更多检查。

---

## 流程控制：if / switch

C++ 的流程控制语句与 C/Java 几乎完全一致。

### if / else if / else

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

### switch

`switch` 用于多分支判断，表达式必须是整型或枚举类型。每个 `case` 通常以 `break` 结束，否则会“贯穿”到下一个 case。

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

> **不要漏写 break**
>
> 忘记 `break` 会导致程序继续执行下一个 case 的代码，这是初学者最容易犯的错误之一。

### C++17 的 if 带初始化语句

C++17 允许在 `if` 中声明变量，这个变量只在 `if` 的作用域内有效：

```cpp
#include <iostream>

int main() {
    if (int x = 42; x > 0) {
        std::cout << "x 是正数：" << x << "\n";
    }
    // x 在这里不可见
    return 0;
}
```

---

## 循环：for / while / do-while / range-based for

循环是本章的重点。C++ 提供了三种传统循环，外加 C++11 引入的基于范围的 for 循环。

### for 循环

C++ 的 `for` 循环语法与 C 完全一致：

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

输出：

```text
0 1 2 3 4
```

> **建议**
>
> 在循环自增时，优先使用 `++i` 而不是 `i++`。对于整数类型两者结果相同，但 `++i` 不会创建临时对象，语义更干净。

### while 循环

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

### do-while 循环

`do-while` 至少执行一次循环体，条件在末尾判断：

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

### break 与 continue

- `break`：立即跳出当前循环。
- `continue`：跳过本次循环剩余代码，进入下一次迭代。

```cpp
#include <iostream>

int main() {
    for (int i = 0; i < 10; ++i) {
        if (i == 3) continue; // 跳过 3
        if (i == 7) break;    // 到 7 时结束循环
        std::cout << i << " ";
    }
    std::cout << "\n";
    return 0;
}
```

输出：

```text
0 1 2 4 5 6
```

### 基于范围的 for 循环（C++11/17）

这是现代 C++ 最常用的循环形式之一，类似 Java 的增强 for 循环：

```cpp
#include <iostream>
#include <vector>

int main() {
    std::vector<int> nums = {1, 2, 3, 4, 5};

    for (int n : nums) {
        std::cout << n << " ";
    }
    std::cout << "\n";

    // 使用 auto 和引用避免拷贝
    for (auto& n : nums) {
        n *= 2;
    }

    for (const auto& n : nums) {
        std::cout << n << " ";
    }
    std::cout << "\n";

    return 0;
}
```

> **与 Java 对比**
>
> Java：`for (int n : nums) { ... }`
>
> C++：`for (int n : nums) { ... }`
>
> 两者语法几乎一样！不过 C++ 中更常用 `auto&` 或 `const auto&` 来避免元素拷贝。

### 循环选择指南

| 场景 | 推荐循环 | 原因 |
|------|----------|------|
| 已知迭代次数 | `for` | 初始化、条件、增量集中在一行 |
| 条件驱动，可能零次 | `while` | 先判断再执行 |
| 至少执行一次 | `do-while` | 先执行再判断 |
| 遍历容器 | `range-based for` | 语法简洁，不易越界 |

---

## 综合练习

完成以下练习，把每道题保存为单独的 `.cpp` 文件，编译并运行。建议先自己思考，再参考提示。

### 练习 1：温度转换

输入一个摄氏温度，输出对应的华氏温度。公式：`F = C × 9/5 + 32`。

### 练习 2：判断闰年

输入一个年份，判断是否为闰年。闰年条件：能被 4 整除但不能被 100 整除，或者能被 400 整除。

### 练习 3：九九乘法表

使用嵌套 `for` 循环打印 9×9 乘法表。

### 练习 4：求素数

输入一个正整数 n，输出 2 到 n 之间的所有素数。

### 练习 5：反转整数

输入一个整数，输出它的反转形式。例如输入 12345，输出 54321。

### 练习提示

```cpp
int n = 12345;
int reversed = 0;

while (n != 0) {
    int digit = n % 10;
    reversed = reversed * 10 + digit;
    n /= 10;
}
std::cout << reversed << "\n";
```

---

## 本章小结

这一章我们完成了 C++ 的入门阶段：

- 了解了 C++ 与 Java、C 的核心差异。
- 学会了编译命令 `g++ -std=c++17`。
- 掌握了变量、常量、基本数据类型和 `auto` 推导。
- 学会了 `std::cin` 和 `std::cout` 的输入输出。
- 熟悉了算术、关系、逻辑、自增、复合赋值运算符。
- 理解了隐式转换与 `static_cast`。
- 掌握了 `if`、`switch`、`for`、`while`、`do-while` 和基于范围的 `for`。

下一章可以继续学习数组、字符串、函数、指针与引用，然后进入面向对象部分。

---

*本教程使用 C++17 标准编写，所有示例代码均可在支持 C++17 的编译器上编译运行。*
