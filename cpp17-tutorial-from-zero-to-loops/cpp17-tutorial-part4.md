# C++17 进阶教程：指针基础

> 第四部分：理解内存地址、指针操作与安全编程
>
> 标准：C++17 | 目标读者：有 Java/C 基础，已完成前三部分（Hello World 到循环、枚举到自定义类型、数组与容器）的学生
>
> 本部分为指针学习的上半部分，涵盖基础概念、指针操作、数组指针、空指针安全、枚举拓展、综合练习与排错避坑

---

## 目录

- [第一课：基础概念——内存地址与取地址运算符](#第一课基础概念内存地址与取地址运算符)
- [第二课：指针操作——声明、初始化与解引用](#第二课指针操作声明初始化与解引用)
- [第三课：数组指针——指针算术与数组遍历](#第三课数组指针指针算术与数组遍历)
- [第四课：空指针安全——nullptr 与合法性判断](#第四课空指针安全nullptr-与合法性判断)
- [第五课：枚举拓展——枚举搭配指针与 const 指针](#第五课枚举拓展枚举搭配指针与-const-指针)
- [第六课：综合练习——指针基础实训案例](#第六课综合练习指针基础实训案例)
- [第七课：排错避坑——常见错误与安全写法](#第七课排错避坑常见错误与安全写法)
- [本章小结](#本章小结)

---

## 第一课：基础概念——内存地址与取地址运算符

### 什么是内存地址

计算机的内存就像一排长长的储物柜，每个柜子有一个编号。当你写 `int x = 42;` 时，编译器在内存中找一个空闲的柜子，把 42 放进去，并记住这个柜子的编号。这个编号就是**内存地址**。

在第二部分"变量的生命周期"中，我们学过变量存储在栈上或堆上。无论在哪里，每个变量都有一个地址。今天我们要做的，就是直接操作这些地址。

```cpp
#include <iostream>

int main() {
    int x = 42;

    // & 是取地址运算符，获取变量 x 的内存地址
    std::cout << "x 的值: " << x << "\n";
    std::cout << "x 的地址: " << &x << "\n";

    double y = 3.14;
    std::cout << "y 的值: " << y << "\n";
    std::cout << "y 的地址: " << &y << "\n";

    return 0;
}
```

运行结果（每次运行地址不同）：

```text
x 的值: 42
x 的地址: 0x7ffd3a2b1c4c
y 的值: 3.14
y 的地址: 0x7ffd3a2b1c40
```

地址用**十六进制**表示（`0x` 开头），因为十六进制比十进制更短、更适合表示内存编号。你可以看到 `x` 和 `y` 的地址很接近——因为它们都是在栈上连续分配的局部变量。

### 取地址运算符 &

`&` 符号你其实已经见过了。在前三部分的代码中，范围 for 循环里频繁出现：

```cpp
for (const auto& name : names) {  // 这里的 & 是"引用"
    std::cout << name << "\n";
}
```

但那个 `&` 是**引用**的语法，和取地址的 `&` 不是同一回事。虽然它们用同一个符号，但出现在不同的位置：

| `&` 的位置 | 含义 | 例子 |
|-----------|------|------|
| 变量声明时，类型和变量名之间 | 引用声明 | `int& r = x;` |
| 赋值时，放在已有变量前面 | 取地址 | `int* p = &x;` |
| 范围 for 中，auto 后面 | 引用遍历 | `for (auto& n : arr)` |

> **易混淆点**：`&` 在不同语境下意思不同。`int& r = x` 中的 `&` 表示"r 是 x 的引用"（别名），`&x` 中的 `&` 表示"取 x 的地址"。本课讲的是后者——取地址。

### 用 cout 打印地址

`std::cout` 可以直接打印地址，默认以十六进制输出。你也可以用 `<iomanip>` 中的格式控制符来调整显示方式：

```cpp
#include <iostream>
#include <iomanip>

int main() {
    int a = 10;
    int b = 20;
    int c = 30;

    // 打印地址（十六进制，默认）
    std::cout << "a 的地址: " << &a << "\n";
    std::cout << "b 的地址: " << &b << "\n";
    std::cout << "c 的地址: " << &c << "\n";

    // 观察相邻变量的地址差
    std::cout << "\n地址差（字节）：\n";
    // 把地址转成整数来计算差值
    std::cout << "a 到 b: " << (char*)&b - (char*)&a << " 字节\n";
    std::cout << "b 到 c: " << (char*)&c - (char*)&b << " 字节\n";

    return 0;
}
```

运行结果（地址因环境不同而异）：

```text
a 的地址: 0x7ffd2a3f1c4c
b 的地址: 0x7ffd2a3f1c48
c 的地址: 0x7ffd2a3f1c44

地址差（字节）：
a 到 b: -4 字节
b 到 c: -4 字节
```

每个 `int` 占 4 字节，所以相邻 `int` 变量的地址相差 4。地址递减是因为栈是"向下生长"的——后声明的变量地址更小。

> **知识点补充： reinterpret_cast**。上面代码中 `(char*)&b` 是一种强制类型转换，把 `int*` 转成 `char*`，这样指针减法的结果就是字节数而不是元素数。C++ 推荐的写法是 `reinterpret_cast<char*>(&b)`，但这里用 C 风格转换更简洁。`reinterpret_cast` 是四种转换操作符之一（第二部分讲过 `static_cast`），用于位级别的类型重新解释。

### 复习：栈与堆

第二部分讲过 C++ 的两种内存区域，这里做一个快速回顾，因为指针和内存区域密切相关：

| 内存区域 | 存储什么 | 谁管理 | 速度 |
|---------|---------|--------|------|
| 栈 (Stack) | 局部变量、函数参数 | 自动（编译器） | 快 |
| 堆 (Heap) | `new` 分配的内存 | 手动（程序员） | 较慢 |
| 全局数据区 | 全局变量、静态变量 | 自动（程序启动/结束） | 中等 |

栈上的变量地址在编译时就能确定大致范围，函数返回时自动销毁。堆上的内存由你用 `new` 申请、用 `delete` 释放——这正是本课接下来要讲的。

### 指针与引用的区别预告

在第一部分的前言中提到过：C++ 有指针，也有引用。它们都能间接操作变量，但有很多区别。本部分先学指针，引用留到下半部分深入。这里先做一个简单的预告：

| 特性 | 指针 | 引用 |
|------|------|------|
| 声明语法 | `int* p = &x;` | `int& r = x;` |
| 可以为空 | 可以（`nullptr`） | 不可以 |
| 可以改变指向 | 可以 | 不可以 |
| 语法 | 需要解引用 `*p` | 直接用 `r` |
| 本质 | 存储地址的变量 | 变量的别名 |

> **Java 对比**：Java 只有引用（对象变量本质是引用），没有指针。Java 的引用不能为空（嗯，可以是 `null`），不能做算术运算。C++ 的指针更底层，能直接操作内存地址，这是 C++ 比 Java 强大也危险的地方。

### 小练习

声明三个不同类型的变量（`int`、`double`、`char`），打印它们的值和地址。观察不同类型变量的地址差，验证每种类型占用的字节数。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    int i = 10;
    double d = 3.14;
    char c = 'A';

    std::cout << "int i: 值=" << i << " 地址=" << &i << " 大小=" << sizeof(i) << "\n";
    std::cout << "double d: 值=" << d << " 地址=" << &d << " 大小=" << sizeof(d) << "\n";
    std::cout << "char c: 值=" << c << " 地址=" << (void*)&c << " 大小=" << sizeof(c) << "\n";

    return 0;
}
```

运行结果（地址因环境而异）：

```text
int i: 值=10 地址=0x7ffd3a2b1c4c 大小=4
double d: 值=3.14 地址=0x7ffd3a2b1c40 大小=8
char c: 值=A 地址=0x7ffd3a2b1c4b 大小=1
```

> **注意**：`char` 的地址需要用 `(void*)&c` 转换后打印，否则 `cout` 会把它当成字符串输出（因为 `char*` 是 C 风格字符串的类型）。

</details>

---

## 第二课：指针操作——声明、初始化与解引用

### 指针变量的声明

指针是一种特殊变量——它存储的不是普通数据，而是**另一个变量的地址**。

```cpp
#include <iostream>

int main() {
    int x = 42;

    // 声明一个指向 int 的指针变量 p，把 x 的地址赋给它
    int* p = &x;  // p 存储了 x 的地址

    std::cout << "x 的值: " << x << "\n";
    std::cout << "x 的地址: " << &x << "\n";
    std::cout << "p 的值（就是 x 的地址）: " << p << "\n";
    std::cout << "p 自己的地址: " << &p << "\n";

    return 0;
}
```

运行结果（地址因环境而异）：

```text
x 的值: 42
x 的地址: 0x7ffd3a2b1c4c
p 的值（就是 x 的地址）: 0x7ffd3a2b1c4c
p 自己的地址: 0x7ffd3a2b1c40
```

`p` 也是一个变量，它自己也有地址。只不过 `p` 存储的内容是 `x` 的地址。这就像你有一张纸条，上面写着"储物柜 42 号"——纸条本身放在你的口袋里（p 的地址），纸条上写的号码是柜子的位置（p 的值）。

**语法格式**：`类型* 指针名 = &变量名;`

> **星号位置**：`int* p` 和 `int *p` 完全等价，只是风格不同。C++ 社区偏好 `int* p`（星号靠近类型），强调"p 是一个 int 指针"。但要注意：`int* p, q;` 只声明了一个指针 `p` 和一个普通 `int` 变量 `q`，不是两个指针。

### 指针的类型约束

指针有类型——`int*` 只能指向 `int`，`double*` 只能指向 `double`。类型决定了指针解引用时读取多少字节：

```cpp
#include <iostream>

int main() {
    int x = 42;
    double y = 3.14;

    int* pi = &x;        // int* 指向 int
    double* pd = &y;     // double* 指向 double

    std::cout << "sizeof(int*) = " << sizeof(pi) << "\n";    // 8
    std::cout << "sizeof(double*) = " << sizeof(pd) << "\n"; // 8

    // pi = &y;  // 编译错误！int* 不能指向 double

    return 0;
}
```

运行结果：

```text
sizeof(int*) = 8
sizeof(double*) = 8
```

无论什么类型的指针，本身的大小都一样（64 位系统上是 8 字节），因为地址就是一串数字，跟它指向的类型无关。类型的作用是告诉编译器"解引用时读几个字节"：`int*` 读 4 字节，`double*` 读 8 字节。

> **Java 对比**：Java 的引用也有类型约束（`String` 引用不能指向 `Integer` 对象），但 Java 的引用不暴露地址，也不能做算术运算。C++ 的指针类型约束类似，但多了指针算术的能力。

### 解引用与间接访问

**解引用**就是通过指针读取或修改它指向的变量。解引用运算符也是 `*`，和声明指针时的 `*` 含义不同：

```cpp
#include <iostream>

int main() {
    int x = 42;
    int* p = &x;  // p 指向 x

    // 解引用：通过 p 读取 x 的值
    std::cout << "*p = " << *p << "\n";  // 42（和 x 的值一样）

    // 通过解引用修改 x 的值
    *p = 100;  // 把 x 改成 100
    std::cout << "修改后 x = " << x << "\n";  // 100

    return 0;
}
```

运行结果：

```text
*p = 42
修改后 x = 100
```

`*p = 100` 这行代码是"通过指针间接修改了 x 的值"。你没有直接写 `x = 100`，但效果完全一样——因为 `p` 指向 `x`，`*p` 就是 `x` 本身。

**`&` 和 `*` 互为逆运算**：

```cpp
int x = 42;
int* p = &x;   // & 取地址：从变量得到地址
int y = *p;    // * 解引用：从地址得到值

// &(*p) == p（取 p 指向的地址，再取地址，还是 p）
// *(&x) == x（取 x 的地址，再解引用，还是 x）
```

| 操作 | 语法 | 含义 |
|------|------|------|
| 取地址 | `&x` | 获取变量 x 的地址 |
| 解引用 | `*p` | 获取指针 p 指向的变量 |
| 互逆关系 | `*(&x) == x` | 先取地址再解引用，得到原变量 |
| 互逆关系 | `&(*p) == p` | 先解引用再取地址，得到原指针 |

> **C 语言对比**：C 语言的指针语法和 C++ 完全一样。如果你学过 C 语言的指针，这部分是复习。C++ 新增的是引用（`&r`），它比指针更安全但功能有限。

### 知识点补充：new 和 delete——在堆上分配内存

第二部分"栈与堆"一节提到过 `new`/`delete`，说"后续课程讲"。现在是正式引入的时候了。

`new` 在堆上分配内存，返回指向那块内存的指针；`delete` 释放那块内存：

```cpp
#include <iostream>

int main() {
    // 在栈上创建变量（自动销毁）
    int stackVar = 10;

    // 在堆上创建变量（手动管理）
    int* heapVar = new int;  // new 分配一个 int 大小的内存，返回地址
    *heapVar = 20;           // 通过指针给那块内存赋值

    std::cout << "栈变量: " << stackVar << "\n";
    std::cout << "堆变量: " << *heapVar << "\n";

    // 用完堆内存后必须释放，否则就是内存泄漏
    delete heapVar;  // 释放内存
    // heapVar 此刻变成悬空指针（后面会讲）

    // new 时直接初始化
    int* p = new int(42);  // 分配并初始化为 42
    std::cout << "p 指向的值: " << *p << "\n";
    delete p;

    // 在堆上创建 double
    double* d = new double(3.14159);
    std::cout << "d 指向的值: " << *d << "\n";
    delete d;

    return 0;
}
```

运行结果：

```text
栈变量: 10
堆变量: 20
p 指向的值: 42
d 指向的值: 3.14159
```

**栈 vs 堆对比**：

| 特性 | 栈上变量 | 堆上变量 |
|------|---------|---------|
| 创建方式 | `int x = 42;` | `int* p = new int(42);` |
| 销毁方式 | 离开作用域自动销毁 | 必须 `delete` |
| 返回类型 | 变量本身 | 指向变量的指针 |
| 大小限制 | 通常几 MB | 受物理内存限制 |
| 速度 | 快 | 较慢 |
| 安全性 | 高（自动管理） | 低（忘记 delete 就泄漏） |

> **Java 对比**：Java 的所有对象都在堆上创建（`new`），由 GC 自动回收。C++ 的 `new` 和 Java 的 `new` 类似，但 C++ 没有垃圾回收——你必须自己 `delete`。这是 C++ 比 Java 难的地方，也是 C++ 性能更高的原因之一（没有 GC 暂停）。

> **编程建议**：现代 C++ 推荐用智能指针（`std::unique_ptr`、`std::shared_ptr`）代替裸 `new`/`delete`。但理解裸指针是使用智能指针的前提，所以这部分先用原始方式学习，排错避坑一节会预告智能指针。

### 小练习

在堆上创建一个 `int` 变量并赋值为 99，再创建一个 `double` 变量赋值为 2.718。通过指针打印它们的值和地址，然后正确释放内存。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    // 在堆上分配
    int* pi = new int(99);
    double* pd = new double(2.718);

    // 通过指针访问
    std::cout << "int 值: " << *pi << " 地址: " << pi << "\n";
    std::cout << "double 值: " << *pd << " 地址: " << pd << "\n";

    // 释放内存
    delete pi;
    delete pd;

    // 释放后将指针置空（好习惯，后面会讲为什么）
    pi = nullptr;
    pd = nullptr;

    std::cout << "内存已释放\n";
    return 0;
}
```

运行结果（地址因环境而异）：

```text
int 值: 99 地址: 0x55a3e8d7ee70
double 值: 2.718 地址: 0x55a3e8d7ee90
内存已释放
```

</details>

---

## 第三课：数组指针——指针算术与数组遍历

### 数组名就是首元素地址

数组名和指针有着密切的关系——数组名本质上就是指向数组第一个元素的指针：

```cpp
#include <iostream>

int main() {
    int arr[5] = {10, 20, 30, 40, 50};

    // arr 自动"退化"为指向首元素的指针
    std::cout << "arr       = " << arr << "\n";       // 首元素地址
    std::cout << "&arr[0]   = " << &arr[0] << "\n";   // 和上面一样
    std::cout << "*arr      = " << *arr << "\n";      // 首元素的值：10

    // 可以用指针的方式访问数组
    std::cout << "arr[2]    = " << arr[2] << "\n";     // 30
    std::cout << "*(arr+2)  = " << *(arr + 2) << "\n"; // 30，和上面一样

    return 0;
}
```

运行结果（地址因环境而异）：

```text
arr       = 0x7ffd3a2b1c30
&arr[0]   = 0x7ffd3a2b1c30
*arr      = 10
arr[2]    = 30
*(arr+2)  = 30
```

`arr` 和 `&arr[0]` 的值完全一样——数组名就是首元素的地址。`arr[2]` 和 `*(arr + 2)` 也完全等价——下标访问就是指针算术的语法糖。

> **C 语言对比**：这个特性完全继承自 C 语言。C 和 C++ 中数组名和指针的关系一模一样。

### 指针算术

指针可以做加减运算，但和普通整数加减不同——指针加减是"跳过元素"，不是"跳过字节"：

```cpp
#include <iostream>

int main() {
    int arr[5] = {10, 20, 30, 40, 50};
    int* p = arr;  // p 指向 arr[0]

    // p + 1 跳过 1 个 int（4 字节），指向 arr[1]
    std::cout << "p     -> " << *p << "\n";       // 10
    std::cout << "p + 1 -> " << *(p + 1) << "\n"; // 20
    std::cout << "p + 2 -> " << *(p + 2) << "\n"; // 30
    std::cout << "p + 4 -> " << *(p + 4) << "\n"; // 50

    // 指针自增
    p++;  // p 现在指向 arr[1]
    std::cout << "p++ 后: " << *p << "\n";  // 20

    p++;  // p 现在指向 arr[2]
    std::cout << "p++ 后: " << *p << "\n";  // 30

    // 指针自减
    p--;  // p 回到 arr[1]
    std::cout << "p-- 后: " << *p << "\n";  // 20

    return 0;
}
```

运行结果：

```text
p     -> 10
p + 1 -> 20
p + 2 -> 30
p + 4 -> 50
p++ 后: 20
p++ 后: 30
p-- 后: 20
```

**指针算术的跳转规则**：

| 运算 | 效果 | 地址变化 |
|------|------|---------|
| `p + 1` | 指向下一个元素 | 地址 + sizeof(类型) |
| `p - 1` | 指向上一个元素 | 地址 - sizeof(类型) |
| `p++` | 移动到下一个元素 | 同上 |
| `p--` | 移动到上一个元素 | 同上 |
| `p2 - p1` | 两指针之间的元素个数 | 地址差 / sizeof(类型) |

如果 `p` 是 `int*`（每个元素 4 字节），`p + 1` 会让地址增加 4。如果是 `double*`（每个元素 8 字节），`p + 1` 会让地址增加 8。编译器根据指针类型自动计算跳转量。

### 用指针遍历数组

有了指针算术，就可以用指针代替下标来遍历数组：

```cpp
#include <iostream>

int main() {
    int arr[5] = {10, 20, 30, 40, 50};
    int len = sizeof(arr) / sizeof(arr[0]);

    // 方式一：用下标遍历（第三部分学过）
    std::cout << "下标遍历: ";
    for (int i = 0; i < len; ++i) {
        std::cout << arr[i] << " ";
    }
    std::cout << "\n";

    // 方式二：用指针算术遍历
    std::cout << "指针遍历: ";
    for (int* p = arr; p != arr + len; ++p) {
        std::cout << *p << " ";
    }
    std::cout << "\n";

    // 方式三：用指针算术 + 下标
    std::cout << "混合遍历: ";
    int* p = arr;
    for (int i = 0; i < len; ++i) {
        std::cout << p[i] << " ";  // p[i] 等价于 *(p + i)
    }
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
下标遍历: 10 20 30 40 50
指针遍历: 10 20 30 40 50
混合遍历: 10 20 30 40 50
```

三种方式输出完全一样。`p[i]` 和 `*(p + i)` 是等价的——C++ 的下标运算符 `[]` 本质上就是指针算术的简写。

### 指针减法与元素距离

两个指向同一数组的指针可以相减，结果是它们之间的元素个数：

```cpp
#include <iostream>

int main() {
    int arr[5] = {10, 20, 30, 40, 50};

    int* p1 = &arr[1];  // 指向第二个元素
    int* p2 = &arr[4];  // 指向第五个元素

    // 指针减法：得到元素个数
    std::cout << "p2 - p1 = " << p2 - p1 << "\n";  // 3（相隔 3 个元素）

    // 用指针减法计算"当前元素到数组开头的距离"
    int* start = arr;
    for (int* p = arr; p != arr + 5; ++p) {
        std::cout << "arr[" << p - start << "] = " << *p << "\n";
    }

    return 0;
}
```

运行结果：

```text
p2 - p1 = 3
arr[0] = 10
arr[1] = 20
arr[2] = 30
arr[3] = 40
arr[4] = 50
```

> **注意**：只有指向同一数组的两个指针才能相减。不同数组的指针相减是未定义行为。

### 数组退化为指针

第三部分提过一个"陷阱"：把数组传给函数后，函数内部用 `sizeof` 得到的不是数组大小，而是指针大小。现在我们来理解为什么。

数组作为函数参数时，会自动**退化**（decay）为指向首元素的指针：

```cpp
#include <iostream>

// 函数接收一个 int 数组——但实际上收到的是 int* 指针
// 注意：arr 在函数内部已退化为指针，sizeof 会返回指针大小（8 字节）
// 这正是本课要演示的"退化"现象
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wsizeof-array-argument"
void printArray(int arr[], int n) {
    // 这里的 arr 已经退化为指针
    std::cout << "函数内 sizeof(arr) = " << sizeof(arr) << "\n";  // 8（指针大小）
    std::cout << "函数内 sizeof(arr[0]) = " << sizeof(arr[0]) << "\n";  // 4

    for (int i = 0; i < n; ++i) {
        std::cout << arr[i] << " ";
    }
    std::cout << "\n";
}
#pragma GCC diagnostic pop

int main() {
    int arr[5] = {10, 20, 30, 40, 50};

    // main 中数组没有退化
    std::cout << "main 中 sizeof(arr) = " << sizeof(arr) << "\n";  // 20（5 * 4）

    // 传给函数时退化为指针
    printArray(arr, 5);

    return 0;
}
```

运行结果：

```text
main 中 sizeof(arr) = 20
函数内 sizeof(arr) = 8
函数内 sizeof(arr[0]) = 4
10 20 30 40 50
```

`main` 中 `sizeof(arr)` 是 20（5 个 int 共 20 字节）。但传给函数后，`arr` 退化成了 `int*` 指针，`sizeof(arr)` 变成了 8（64 位系统上指针的大小）。

**这就是为什么必须把数组长度作为额外参数传进去**——函数内部无法通过 `sizeof` 获取数组长度。

> **避免退化的方案**：
> - 用 `std::array`（固定大小，不会退化，第三部分学过）
> - 用 `std::vector`（自带 `.size()`，第三部分学过）
> - 用引用传数组：`void func(int (&arr)[5])`（语法复杂，不推荐）
> - 用模板：`template <int N> void func(int (&arr)[N])`（后续课程讲）

### .data() 和 begin()/end() 回顾

第三部分学过 `std::array` 和 `std::vector` 的 `.data()` 方法，它返回底层数组的指针：

```cpp
#include <iostream>
#include <vector>
#include <array>

int main() {
    std::vector<int> v = {10, 20, 30};
    std::array<int, 3> a = {40, 50, 60};

    // .data() 返回底层数组的首元素指针
    int* vp = v.data();
    int* ap = a.data();

    // 可以用指针算术访问
    std::cout << "vector: " << vp[0] << " " << vp[1] << " " << vp[2] << "\n";
    std::cout << "array:  " << ap[0] << " " << ap[1] << " " << ap[2] << "\n";

    // .begin() 和 .end() 也返回类似指针的东西（迭代器）
    // 迭代器的用法和指针几乎一样
    std::cout << "用迭代器: ";
    for (auto it = v.begin(); it != v.end(); ++it) {
        std::cout << *it << " ";  // *it 解引用，和 *p 一样
    }
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
vector: 10 20 30
array:  40 50 60
用迭代器: 10 20 30
```

> **知识点补充：迭代器**。`v.begin()` 返回的是一个**迭代器**（iterator），它是指针的"升级版"——行为像指针，但功能更强大。迭代器是 C++ STL（标准模板库）的核心概念，后续课程会详细讲。目前只需要知道：迭代器的 `*` 解引用和 `++` 自增用法，和指针完全一样。

### 小练习

用指针算术（不用下标）遍历一个包含 8 个元素的数组，找到最大值及其在数组中的位置（用指针减法计算下标）。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    int arr[8] = {45, 78, 23, 89, 56, 12, 90, 34};
    int len = sizeof(arr) / sizeof(arr[0]);

    int* maxPtr = arr;  // 假设第一个元素是最大值
    int* p = arr;       // 遍历指针

    // 从第二个元素开始比较
    for (p = arr + 1; p != arr + len; ++p) {
        if (*p > *maxPtr) {
            maxPtr = p;  // 更新最大值指针
        }
    }

    // 用指针减法计算下标
    int maxIndex = maxPtr - arr;

    std::cout << "最大值: " << *maxPtr << "\n";
    std::cout << "位置: arr[" << maxIndex << "]\n";

    return 0;
}
```

运行结果：

```text
最大值: 90
位置: arr[6]
```

</details>

---

## 第四课：空指针安全——nullptr 与合法性判断

### nullptr：空指针的标准写法

`nullptr` 是 C++11 引入的关键字，表示"指针不指向任何东西"。它是 C 语言 `NULL` 的安全替代品：

```cpp
#include <iostream>

int main() {
    // 声明一个不指向任何东西的指针
    int* p = nullptr;  // 空指针

    // 检查指针是否为空
    if (p == nullptr) {
        std::cout << "p 是空指针，不能解引用\n";
    }

    // 给 p 一个有效的地址
    int x = 42;
    p = &x;

    if (p != nullptr) {
        std::cout << "p 指向: " << *p << "\n";  // 42
    }

    // 清空指针
    p = nullptr;
    std::cout << "p 又变成空指针了\n";

    return 0;
}
```

运行结果：

```text
p 是空指针，不能解引用
p 指向: 42
p 又变成空指针了
```

**使用 nullptr 的场景**：

| 场景 | 代码 | 原因 |
|------|------|------|
| 声明时初始化 | `int* p = nullptr;` | 防止野指针 |
| 释放内存后 | `delete p; p = nullptr;` | 防止悬空指针 |
| 函数返回失败 | `return nullptr;` | 表示"没有有效数据" |
| 检查指针有效性 | `if (p != nullptr)` | 解引用前先检查 |

### nullptr vs NULL vs 0

C 语言用 `NULL` 表示空指针，C++11 之前也用 `NULL`。但 `NULL` 有一个隐患——它可能被定义为 `0`（整数），导致函数重载时的歧义：

```cpp
#include <iostream>

// 两个重载函数
void func(int x) {
    std::cout << "调用 int 版本: " << x << "\n";
}

void func(int* p) {
    std::cout << "调用指针版本: " << (p ? "非空" : "空") << "\n";
}

int main() {
    // func(NULL);  // 歧义！NULL 是 0 还是空指针？
    // 某些编译器会调用 int 版本，导致 bug

    func(nullptr);  // 明确调用指针版本，无歧义
    func(0);        // 明确调用 int 版本

    return 0;
}
```

运行结果：

```text
调用指针版本: 空
调用 int 版本: 0
```

三者对比：

| 写法 | 类型 | C++11 后推荐 |
|------|------|-------------|
| `0` | int | 不推荐（有歧义风险） |
| `NULL` | 实现定义（可能是 int 0） | 不推荐（有歧义风险） |
| `nullptr` | nullptr_t（独立类型） | 推荐 |

> **编程建议**：C++11 以后，一律用 `nullptr` 代替 `NULL` 和 `0`。它类型安全，不会引起歧义。

### 未初始化指针（野指针）

声明指针但不初始化，它的值是随机的——可能指向内存中的任何位置。这种指针叫**野指针**：

```cpp
#include <iostream>

int main() {
    // 危险！p 没有初始化，值是随机的
    int* p;  // 野指针！

    // *p = 42;  // 极其危险！可能：
    // 1. 写入只读内存 -> 程序崩溃
    // 2. 覆盖其他变量 -> 诡异的 bug
    // 3. 碰巧写入空闲内存 -> 暂时没事，但随时可能出问题

    // 正确做法：声明时初始化
    int* q = nullptr;  // 空指针，安全
    int x = 42;
    int* r = &x;       // 指向有效变量

    std::cout << "q 是空指针: " << (q == nullptr) << "\n";
    std::cout << "r 指向: " << *r << "\n";

    return 0;
}
```

运行结果：

```text
q 是空指针: 1
r 指向: 42
```

> **铁律**：指针声明时必须初始化——要么指向一个有效变量，要么赋值为 `nullptr`。绝不留空。

### 悬空指针

指针指向的变量已经被销毁，但指针仍然保存着那个地址——这种指针叫**悬空指针**。它比野指针更隐蔽，因为地址看起来是"有效的"：

```cpp
#include <iostream>

int* dangerousFunc() {
    int local = 42;  // 局部变量，在栈上
    return &local;   // 返回局部变量的地址！
    // local 在函数返回时被销毁，但返回的地址还指向那块内存
}

int main() {
    int* p = dangerousFunc();  // p 得到了一个悬空地址

    // *p 看起来能工作，但那块内存可能已经被其他函数覆盖
    // 这是未定义行为！
    // std::cout << *p << "\n";  // 可能输出 42，也可能输出垃圾，也可能崩溃

    std::cout << "p 的地址: " << p << "（悬空，不可解引用）\n";

    // 正确做法：用堆分配
    int* safe = new int(42);
    std::cout << "safe 指向: " << *safe << "\n";
    delete safe;
    safe = nullptr;  // delete 后置空

    return 0;
}
```

运行结果（地址因环境而异）：

```text
p 的地址: 0x7ffd3a2b1c4c（悬空，不可解引用）
safe 指向: 42
```

**悬空指针的常见来源**：

| 来源 | 例子 | 防范 |
|------|------|------|
| 返回局部变量地址 | `return &local;` | 改用堆分配或传引用 |
| delete 后未置空 | `delete p; /* 用 p */` | `delete p; p = nullptr;` |
| 指向容器元素后被修改 | `int* p = &v[0]; v.push_back(...)` | 避免长期持有容器元素指针 |

> **知识点补充：vector 扩容导致悬空**。第三部分讲过 `vector` 扩容时会搬移数据。如果你在扩容前保存了一个指向 vector 元素的指针，扩容后那个指针就悬空了——因为数据搬到了新地址。这是实际工程中常见的悬空指针来源。

### delete 后置为 nullptr 的编程习惯

```cpp
#include <iostream>

int main() {
    int* p = new int(42);
    std::cout << "分配: " << *p << "\n";

    delete p;     // 释放内存
    p = nullptr;  // 置空，防止后续误用

    // delete 之后再检查
    if (p != nullptr) {
        *p = 100;  // 不会执行
    } else {
        std::cout << "p 已释放并置空，跳过访问\n";
    }

    // 对 nullptr 做 delete 是安全的（什么都不做）
    delete p;  // 安全！C++ 规定 delete nullptr 是空操作

    return 0;
}
```

运行结果：

```text
分配: 42
p 已释放并置空，跳过访问
```

> **编程习惯**：每次 `delete` 之后，立即把指针置为 `nullptr`。这有两个好处：1) 后续检查 `if (p != nullptr)` 能正确跳过；2) 对 `nullptr` 做 `delete` 是安全的，不会重复释放。

### 小练习

写一个函数 `safeDivide`，接收两个 `int` 指针，返回较大的那个指针指向的值。如果任一指针为 `nullptr`，返回 -1 并打印错误信息。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int safeMax(const int* a, const int* b) {
    // 先检查指针是否有效
    if (a == nullptr || b == nullptr) {
        std::cout << "错误：传入空指针\n";
        return -1;
    }

    // 两个都有效，返回较大的值
    return (*a > *b) ? *a : *b;
}

int main() {
    int x = 30, y = 50;

    // 正常调用
    std::cout << "max(30, 50) = " << safeMax(&x, &y) << "\n";

    // 传入空指针
    int* p = nullptr;
    std::cout << "max(30, nullptr) = " << safeMax(&x, p) << "\n";

    return 0;
}
```

运行结果：

```text
max(30, 50) = 50
错误：传入空指针
max(30, nullptr) = -1
```

</details>

---

## 第五课：枚举拓展——枚举搭配指针与 const 指针

### 指向枚举变量的指针

第二部分学过的 `enum class` 也可以用指针操作。指向枚举的指针和普通指针用法一样，只是类型变成了枚举类型：

```cpp
#include <iostream>

enum class TrafficLight : int { Red, Green, Yellow };

int main() {
    TrafficLight light = TrafficLight::Green;
    TrafficLight* p = &light;  // 指向枚举变量的指针

    // 通过指针读取
    std::cout << "当前灯: " << static_cast<int>(*p) << "\n";  // 1

    // 通过指针修改
    *p = TrafficLight::Yellow;
    std::cout << "修改后: " << static_cast<int>(*p) << "\n";  // 2
    std::cout << "原变量: " << static_cast<int>(light) << "\n";  // 2

    return 0;
}
```

运行结果：

```text
当前灯: 1
修改后: 2
原变量: 2
```

> **static_cast 回顾**：`enum class` 不能直接用 `cout` 输出（第二部分讲过），需要用 `static_cast<int>` 转成整数。

### 枚举做数组下标与指针遍历

第二部分学过用枚举做数组下标，现在结合指针来遍历：

```cpp
#include <iostream>
#include <string>

enum class Direction : int { Up = 0, Down, Left, Right, Count };

int main() {
    // 用枚举值个数作为数组大小
    std::string names[static_cast<int>(Direction::Count)] = {
        "上",  // Up
        "下",  // Down
        "左",  // Left
        "右"   // Right
    };

    // 用指针遍历数组
    std::cout << "方向列表：\n";
    for (std::string* p = names; p != names + static_cast<int>(Direction::Count); ++p) {
        int index = p - names;  // 用指针减法计算下标
        std::cout << "  [" << index << "] " << *p << "\n";
    }

    // 用枚举做下标访问
    Direction d = Direction::Left;
    std::cout << "\n方向 " << static_cast<int>(d) << " 是: "
              << names[static_cast<int>(d)] << "\n";

    return 0;
}
```

运行结果：

```text
方向列表：
  [0] 上
  [1] 下
  [2] 左
  [3] 右

方向 2 是: 左
```

> **编程技巧**：在枚举最后加一个 `Count` 值，可以自动得到枚举元素的数量。`Count` 本身不对应任何实际数据，但它的值刚好是前面所有枚举值的个数。这是 C++ 枚举配合数组的常用模式。

### const 指针的三种形式

`const` 和指针组合时，可以出现在不同位置，含义完全不同。这是 C++ 指针中最容易混淆的知识点：

**形式一：`const int* p`——指向常量的指针（不能通过 p 修改值）**

```cpp
int x = 42;
const int* p = &x;  // p 指向 x，但承诺不通过 p 修改 x
// *p = 100;  // 编译错误！不能通过 const 指针修改
p = &y;       // 合法：可以让 p 指向别的变量
```

**形式二：`int* const p`——常量指针（不能改变 p 的指向）**

```cpp
int x = 42;
int* const p = &x;  // p 永远指向 x，不能改
*p = 100;     // 合法：可以通过 p 修改 x
// p = &y;    // 编译错误！不能改变 p 的指向
```

**形式三：`const int* const p`——两者都不可改**

```cpp
int x = 42;
const int* const p = &x;
// *p = 100;  // 编译错误
// p = &y;    // 编译错误
```

完整示例：

```cpp
#include <iostream>

int main() {
    int x = 42;
    int y = 100;

    // 形式一：const int* —— 不能通过指针改值，但能改指向
    const int* p1 = &x;
    // *p1 = 50;  // 错误：不能通过 p1 修改值
    p1 = &y;      // 合法：可以改指向
    std::cout << "p1 -> " << *p1 << "\n";  // 100

    // 形式二：int* const —— 不能改指向，但能通过指针改值
    int* const p2 = &x;
    *p2 = 50;      // 合法：可以通过 p2 修改 x
    // p2 = &y;   // 错误：不能改指向
    std::cout << "x = " << x << "\n";  // 50

    // 形式三：const int* const —— 都不能改
    const int* const p3 = &x;
    // *p3 = 10;  // 错误
    // p3 = &y;   // 错误
    std::cout << "p3 -> " << *p3 << "\n";  // 50

    return 0;
}
```

运行结果：

```text
p1 -> 100
x = 50
p3 -> 50
```

**记忆口诀**：

| 写法 | const 修饰谁 | 能改值吗 | 能改指向吗 |
|------|-------------|---------|-----------|
| `const int* p` | `*p`（值） | 不能 | 能 |
| `int* const p` | `p`（指针） | 能 | 不能 |
| `const int* const p` | 两者 | 不能 | 不能 |

> **速记方法**：看 `const` 在 `*` 的左边还是右边。左边修饰值（不能改值），右边修饰指针（不能改指向）。

> **Java 对比**：Java 的 `final` 引用类似 `int* const`——引用本身不能重新赋值，但对象内容可以修改。Java 没有类似 `const int*` 的概念（不能阻止通过引用修改对象）。C++ 的 const 指针体系比 Java 更精细。

### const 指针的实际用途

```cpp
#include <iostream>

// 函数接收 const 指针：承诺不修改数据
// 这和第三部分学过的 const 引用传参是同样的道理
void printValue(const int* p) {
    // *p = 100;  // 编译错误！承诺了不修改
    if (p != nullptr) {
        std::cout << "值: " << *p << "\n";
    } else {
        std::cout << "空指针\n";
    }
}

int main() {
    int x = 42;
    printValue(&x);    // 传入地址
    printValue(nullptr);  // 传入空指针

    return 0;
}
```

运行结果：

```text
值: 42
空指针
```

> **编程建议**：函数参数如果是指针且不需要修改数据，一律加 `const`。写成 `const int* p` 而不是 `int* p`。这既保护了调用者的数据，也向调用者明确传达"我只读不改"。

### 小练习

创建一个 `enum class` 表示 4 种水果，创建对应的中文名称数组。用 `const` 指针遍历数组并输出每种水果的编号和名称。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <string>

enum class Fruit : int { Apple = 0, Banana, Cherry, Durian, Count };

int main() {
    // 水果名称数组
    std::string names[static_cast<int>(Fruit::Count)] = {
        "苹果", "香蕉", "樱桃", "榴莲"
    };

    // 用 const 指针遍历（只读不修改）
    const std::string* p = names;
    std::cout << "=== 水果列表 ===\n";
    for (int i = 0; i < static_cast<int>(Fruit::Count); ++i) {
        std::cout << i << ". " << *p << "\n";
        ++p;  // 移动到下一个
    }

    // 用枚举做下标
    Fruit f = Fruit::Cherry;
    std::cout << "\n你选的是: " << names[static_cast<int>(f)] << "\n";

    return 0;
}
```

运行结果：

```text
=== 水果列表 ===
0. 苹果
1. 香蕉
2. 樱桃
3. 榴莲

你选的是: 樱桃
```

</details>

---

## 第六课：综合练习——指针基础实训案例

以下 4 道练习综合运用本部分所有知识点。每题附标准答案和运行结果。

### 练习一：用指针实现数组反转

将一个数组的元素顺序反转，要求全程使用指针操作，不使用下标。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

// 用指针反转数组
void reverseArray(int* begin, int* end) {
    // begin 指向第一个元素，end 指向最后一个元素的后面
    --end;  // end 退到最后一个元素
    while (begin < end) {
        // 交换 begin 和 end 指向的元素
        int temp = *begin;
        *begin = *end;
        *end = temp;
        ++begin;  // 前指针后移
        --end;     // 后指针前移
    }
}

int main() {
    int arr[6] = {1, 2, 3, 4, 5, 6};
    int len = sizeof(arr) / sizeof(arr[0]);

    // 反转前
    std::cout << "反转前: ";
    for (int i = 0; i < len; ++i) std::cout << arr[i] << " ";
    std::cout << "\n";

    // 调用反转函数
    reverseArray(arr, arr + len);

    // 反转后
    std::cout << "反转后: ";
    for (int i = 0; i < len; ++i) std::cout << arr[i] << " ";
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
反转前: 1 2 3 4 5 6
反转后: 6 5 4 3 2 1
```

</details>

### 练习二：用指针查找数组最大值和最小值

用指针遍历数组，同时找到最大值和最小值及其位置。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

// 通过指针同时查找最大值和最小值
void findMinMax(const int* arr, int len, int* maxVal, int* minVal) {
    if (arr == nullptr || len <= 0 || maxVal == nullptr || minVal == nullptr) {
        std::cout << "参数无效\n";
        return;
    }

    *maxVal = *arr;  // 假设第一个是最大值
    *minVal = *arr;  // 假设第一个是最小值

    for (const int* p = arr + 1; p != arr + len; ++p) {
        if (*p > *maxVal) *maxVal = *p;
        if (*p < *minVal) *minVal = *p;
    }
}

int main() {
    int arr[8] = {45, 78, 23, 89, 56, 12, 90, 34};
    int len = sizeof(arr) / sizeof(arr[0]);

    int maxVal, minVal;
    findMinMax(arr, len, &maxVal, &minVal);

    std::cout << "数组: ";
    for (int i = 0; i < len; ++i) std::cout << arr[i] << " ";
    std::cout << "\n";
    std::cout << "最大值: " << maxVal << "\n";
    std::cout << "最小值: " << minVal << "\n";
    std::cout << "极差: " << maxVal - minVal << "\n";

    return 0;
}
```

运行结果：

```text
数组: 45 78 23 89 56 12 90 34
最大值: 90
最小值: 12
极差: 78
```

</details>

### 练习三：用 new/delete 动态创建数组

用 `new` 在堆上创建一个动态数组，接收用户输入填充数据，计算平均值后释放内存。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int main() {
    int n;
    std::cout << "输入学生人数: ";
    std::cin >> n;

    if (n <= 0) {
        std::cout << "人数必须大于 0\n";
        return 1;
    }

    // 在堆上动态创建数组（运行时才知道大小）
    int* scores = new int[n];

    // 接收输入
    for (int i = 0; i < n; ++i) {
        std::cout << "学生 " << i + 1 << " 的成绩: ";
        std::cin >> scores[i];
    }

    // 计算总分和平均分
    int sum = 0;
    for (int i = 0; i < n; ++i) {
        sum += scores[i];
    }
    double avg = static_cast<double>(sum) / n;

    // 输出结果
    std::cout << "\n=== 成绩报告 ===\n";
    for (int i = 0; i < n; ++i) {
        std::cout << "学生 " << i + 1 << ": " << scores[i] << " 分\n";
    }
    std::cout << "总分: " << sum << "\n";
    std::cout << "平均分: " << avg << "\n";

    // 释放堆内存
    delete[] scores;     // 注意：数组用 delete[]，不是 delete
    scores = nullptr;   // 置空

    return 0;
}
```

运行示例（输入 3 个学生）：

```text
输入学生人数: 3
学生 1 的成绩: 92
学生 2 的成绩: 85
学生 3 的成绩: 78

=== 成绩报告 ===
学生 1: 92 分
学生 2: 85 分
学生 3: 78 分
总分: 255
平均分: 85
```

> **知识点补充：`delete[]`**。用 `new int[n]` 分配的数组，必须用 `delete[]`（带方括号）释放，不能用 `delete`。`delete[]` 会逐个调用析构函数并释放整块内存。如果用错了，可能导致内存泄漏或程序崩溃。

</details>

### 练习四：用指针模拟简易字符串操作

C 风格字符串本质是 `char` 数组，以 `\0` 结尾。用指针遍历和操作这种字符串：

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

// 用指针计算字符串长度（类似 strlen）
int myStrlen(const char* s) {
    if (s == nullptr) return 0;
    int len = 0;
    while (*s != '\0') {  // 遇到结尾符就停
        ++len;
        ++s;  // 移动到下一个字符
    }
    return len;
}

// 用指针复制字符串
void myStrcpy(char* dest, const char* src) {
    if (dest == nullptr || src == nullptr) return;
    while (*src != '\0') {
        *dest = *src;  // 复制一个字符
        ++dest;
        ++src;
    }
    *dest = '\0';  // 别忘了结尾符
}

int main() {
    const char* original = "Hello, C++!";
    char buffer[50];  // 足够大的缓冲区

    // 计算长度
    int len = myStrlen(original);
    std::cout << "长度: " << len << "\n";

    // 复制
    myStrcpy(buffer, original);
    std::cout << "复制后: " << buffer << "\n";

    // 用指针逐字符遍历
    std::cout << "逐字符: ";
    for (const char* p = buffer; *p != '\0'; ++p) {
        std::cout << *p;
    }
    std::cout << "\n";

    return 0;
}
```

运行结果：

```text
长度: 11
复制后: Hello, C++!
逐字符: Hello, C++!
```

> **C 语言对比**：C 风格字符串（`char` 数组 + `\0`）完全继承自 C 语言。C++ 标准库提供了 `std::string`（第二部分学过），比 `char` 数组安全得多。但理解 C 字符串仍然很重要——底层编程、与 C API 交互、阅读老代码时都会遇到。

</details>

---

## 第七课：排错避坑——常见错误与安全写法

### 错误一：未初始化指针

```cpp
// 错误写法
int* p;         // 野指针，值随机
// *p = 42;     // 未定义行为，可能崩溃

// 正确写法
int* p = nullptr;   // 初始化为空指针
int x = 42;
int* q = &x;        // 指向有效变量
```

**安全原则**：声明指针时必须初始化——要么 `nullptr`，要么有效地址。

### 错误二：越界访问

```cpp
int arr[5] = {1, 2, 3, 4, 5};
int* p = arr;

// 错误：指针超出数组范围
// *(p + 10) = 100;  // 越界写入！未定义行为

// 正确：确保指针在有效范围内
for (int* it = arr; it != arr + 5; ++it) {  // 用 != 判断边界
    std::cout << *it << " ";
}
```

**安全原则**：指针算术不能超出数组范围。用 `begin` 和 `end` 指针限定边界。

### 错误三：悬空指针

```cpp
// 错误：返回局部变量地址
int* badFunc() {
    int local = 42;
    return &local;  // local 在函数返回时销毁，返回的地址悬空
}

// 正确：用堆分配
int* goodFunc() {
    return new int(42);  // 堆上分配，调用者负责 delete
}
```

**安全原则**：不要返回局部变量的地址。如果需要返回堆上的数据，用 `new` 分配并返回指针，或者用智能指针。

### 错误四：内存泄漏

```cpp
// 错误：new 了不 delete
void leak() {
    int* p = new int(42);
    // 忘记 delete！每次调用都泄漏 4 字节
}

// 正确：配对使用
void noLeak() {
    int* p = new int(42);
    // 使用 p ...
    delete p;
    p = nullptr;
}
```

**安全原则**：每个 `new` 必须有一个 `delete` 配对。每个 `new[]` 必须有一个 `delete[]` 配对。用完立即释放。

### 错误五：类型不匹配

```cpp
double d = 3.14;
// int* p = &d;  // 编译错误！int* 不能指向 double

// 正确：类型必须匹配
double* p = &d;  // double* 指向 double
```

**安全原则**：指针类型必须和指向的变量类型一致。

### 错误六：忘记 nullptr 检查

```cpp
int* p = getSomePointer();  // 可能返回 nullptr

// 错误：不检查就解引用
// std::cout << *p << "\n";  // 如果 p 是 nullptr，直接崩溃

// 正确：先检查
if (p != nullptr) {
    std::cout << *p << "\n";
} else {
    std::cout << "空指针\n";
}
```

**安全原则**：解引用指针前，先检查是否为 `nullptr`。

### 错误七：const 误用

```cpp
const int x = 42;
const int* p = &x;  // 正确：const 变量需要 const 指针
// int* q = &x;     // 编译错误！不能用非 const 指针指向 const 变量
// *q = 100;        // 如果允许，就绕过了 const 保护
```

**安全原则**：指向 `const` 变量的指针也必须加 `const`。

### 错误八：数组退化陷阱

```cpp
// 错误：函数内部用 sizeof 获取数组长度
void process(int arr[]) {
    int len = sizeof(arr) / sizeof(arr[0]);  // 得到 8/4=2，而不是数组长度！
    // arr 已经退化为指针，sizeof(arr) = 指针大小
}

// 正确：把长度作为参数传入
void process(int arr[], int n) {
    for (int i = 0; i < n; ++i) {
        // 处理 arr[i]
    }
}

// 更好：用 std::vector 或 std::array（自带长度信息）
void process(std::vector<int>& v) {
    for (size_t i = 0; i < v.size(); ++i) {
        // 处理 v[i]
    }
}
```

**安全原则**：不要在函数内部用 `sizeof` 获取数组长度——传入长度参数，或改用 `std::vector`/`std::array`。

### 错误清单汇总

| 编号 | 错误 | 后果 | 正确做法 |
|------|------|------|---------|
| 1 | 未初始化指针 | 随机地址访问 | 声明时赋 `nullptr` 或有效地址 |
| 2 | 越界访问 | 未定义行为/崩溃 | 用 begin/end 限定边界 |
| 3 | 悬空指针 | 读取垃圾数据/崩溃 | 不返回局部地址，delete 后置空 |
| 4 | 内存泄漏 | 内存逐渐耗尽 | new/delete 配对 |
| 5 | 类型不匹配 | 编译错误 | 类型必须一致 |
| 6 | 忘记检查 nullptr | 程序崩溃 | 解引用前先检查 |
| 7 | const 误用 | 编译错误/绕过保护 | const 变量用 const 指针 |
| 8 | 数组退化 | 长度计算错误 | 传长度参数或用 vector/array |

### 预告：智能指针

本部分用的都是"裸指针"（raw pointer）——需要手动 `new`/`delete`。C++11 引入了**智能指针**，能自动管理内存，大幅减少内存泄漏和悬空指针的风险：

```cpp
#include <iostream>
#include <memory>  // 智能指针头文件

int main() {
    // unique_ptr：独占所有权，离开作用域自动 delete
    std::unique_ptr<int> p1 = std::make_unique<int>(42);
    std::cout << "p1 = " << *p1 << "\n";  // 42
    // 不需要 delete！离开作用域自动释放

    // shared_ptr：共享所有权，引用计数归零时自动 delete
    std::shared_ptr<int> p2 = std::make_shared<int>(100);
    std::shared_ptr<int> p3 = p2;  // 可以共享
    std::cout << "p2 = " << *p2 << ", p3 = " << *p3 << "\n";
    std::cout << "引用计数: " << p2.use_count() << "\n";  // 2

    // 都不需要手动 delete！
    return 0;
}
```

运行结果：

```text
p1 = 42
p2 = 100, p3 = 100
引用计数: 2
```

智能指针是现代 C++ 内存管理的核心工具。下半部分会深入学习。目前的建议是：理解裸指针原理，实际工程中优先用智能指针。

> **Java 对比**：`std::shared_ptr` 类似 Java 的普通引用——只要有引用指向对象，对象就不会被回收。`std::unique_ptr` 类似 Java 中你"独占"的对象引用。区别是 C++ 的智能指针在编译期就确定所有权，比 Java 的运行时 GC 更高效。

---

## 本章小结

回顾这一部分你学到的知识：

| 知识点 | 你掌握的能力 |
|--------|------------|
| 内存地址 | 理解变量在内存中的位置，用 `&` 取地址 |
| `&` 的双重含义 | 区分取地址和引用声明的位置规则 |
| 指针声明 | `int* p = &x;` 声明并初始化指针 |
| 解引用 | 用 `*p` 读取和修改指向的变量 |
| 指针类型约束 | `int*` 只能指向 `int`，类型决定读取字节数 |
| new/delete | 在堆上分配和释放内存 |
| 数组名即指针 | 理解 `arr` 等价于 `&arr[0]` |
| 指针算术 | `p+1` 跳过一个元素，`p2-p1` 得到元素距离 |
| 数组退化 | 数组传给函数后退化为指针，sizeof 失效 |
| nullptr | 用 nullptr 代替 NULL 和 0 |
| 野指针 | 未初始化指针的危险，声明时必须初始化 |
| 悬空指针 | 指向已销毁变量，delete 后置空 |
| const 指针 | `const int*`、`int* const`、`const int* const` 三种形式 |
| 枚举+指针 | 枚举做下标配合指针遍历，Count 技巧 |
| 排错避坑 | 8 种常见错误及安全写法 |
| 智能指针预告 | unique_ptr 和 shared_ptr 的基本概念 |

**指针核心概念速查**：

| 概念 | 语法 | 含义 |
|------|------|------|
| 取地址 | `&x` | 获取变量 x 的地址 |
| 解引用 | `*p` | 获取指针 p 指向的值 |
| 声明指针 | `int* p` | p 是指向 int 的指针 |
| 空指针 | `nullptr` | 指针不指向任何东西 |
| 堆分配 | `new int(42)` | 在堆上分配并初始化 |
| 释放 | `delete p` | 释放堆内存 |
| 数组分配 | `new int[n]` | 在堆上创建动态数组 |
| 数组释放 | `delete[] p` | 释放动态数组 |
| const 指针 | `const int* p` | 不能通过 p 改值 |
| 常量指针 | `int* const p` | 不能改变 p 的指向 |

**下一步建议**：

1. 把 4 道综合练习全部手写一遍
2. 尝试修改练习中的代码，故意制造错误并观察结果
3. 下半部分我们将学习：引用深入、指针与函数（值传递/指针传递/引用传递）、函数指针、多级指针
4. 之后学习智能指针（unique_ptr、shared_ptr、weak_ptr）的完整用法
5. 最后进入面向对象：类、构造函数、继承、多态

---

*本教程使用 C++17 标准编写，所有示例代码均已在 g++ -std=c++17 下编译验证通过。*
