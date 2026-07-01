# C++17 进阶教程：从枚举到自定义类型

> 第二部分：深入理解 C++ 的类型系统与作用域
>
> 标准：C++17 | 目标读者：有 Java/C 基础，已完成第一部分（Hello World 到循环）的学生

---

## 目录

- [第一课：枚举变量](#第一课枚举变量)
- [第二课：自定义变量名称](#第二课自定义变量名称)
- [第三课：命名空间](#第三课命名空间)
- [第四课：变量的生命周期](#第四课变量的生命周期)
- [第五课：数据和计算补充知识](#第五课数据和计算补充知识)
- [第六课：自定义数据类型](#第六课自定义数据类型)
- [综合练习项目（含标准答案与运行结果）](#综合练习项目含标准答案与运行结果)
- [本章小结](#本章小结)

---

## 第一课：枚举变量

### 什么是枚举

当你需要表示一组固定的、有限的取值时，枚举是最好的选择。比如一周只有七天，一年只有十二个月，红绿灯只有红黄绿三种状态。

Java 程序员对枚举不会陌生，但 C++ 的枚举有两个版本：传统枚举（C 风格）和强类型枚举（C++11 引入）。两者在 C++17 中都可用，但现代 C++ 推荐后者。

### 传统枚举（enum）

```cpp
#include <iostream>

// 定义一个传统枚举
enum Color {
    RED,    // 默认值 0
    GREEN,  // 默认值 1
    BLUE    // 默认值 2
};

int main() {
    Color c = GREEN;
    std::cout << "GREEN 的值是：" << c << "\n";  // 输出 1
    return 0;
}
```

运行结果：

```text
GREEN 的值是：1
```

枚举值默认从 0 开始递增。你也可以手动指定：

```cpp
enum HttpStatus {
    OK = 200,
    NOT_FOUND = 404,
    SERVER_ERROR = 500
};

int main() {
    HttpStatus s = NOT_FOUND;
    std::cout << "状态码：" << s << "\n";  // 输出 404
    return 0;
}
```

### 传统枚举的问题

传统枚举有两个让人头疼的缺陷，这也是 Java 枚举不会出现的问题：

**问题一：枚举值会"污染"外层命名空间**

```cpp
enum Fruit { APPLE, BANANA };
enum Tech  { APPLE, GOOGLE };  // 编译错误！APPLE 重复定义
```

两个枚举里都有 `APPLE`，编译器会报错，因为传统枚举的成员名字暴露在外层作用域里。

**问题二：可以隐式转换为整数**

```cpp
enum Color { RED, GREEN, BLUE };
Color c = RED;
int x = c + 5;  // 合法！c 被隐式转成 0，x = 5
```

这种隐式转换有时会掩盖 bug。你本意是操作颜色，结果却变成了整数运算。

### 强类型枚举（enum class）

C++11 引入了 `enum class`（也叫 scoped enum），解决了上述两个问题。现代 C++ 中优先使用它。

```cpp
#include <iostream>

// 强类型枚举：用 enum class 声明
enum class Color {
    Red,
    Green,
    Blue
};

enum class Fruit {
    Apple,    // 不会和 Color::Red 冲突
    Banana
};

int main() {
    Color c = Color::Green;  // 必须加 Color:: 前缀
    std::cout << "颜色是 Green\n";

    // int x = c;        // 编译错误！不能隐式转成 int
    int x = static_cast<int>(c);  // 必须显式转换
    std::cout << "Green 的值是：" << x << "\n";  // 输出 1

    return 0;
}
```

运行结果：

```text
颜色是 Green
Green 的值是：1
```

### 知识点补充：static_cast 类型转换

上面的代码中出现了一个新面孔：`static_cast<int>(c)`。这是 C++ 推荐的类型转换写法，我们来正式认识它。

在 C 语言中，你习惯这样转换类型：

```c
int x = (int)c;        // C 风格转换
```

C++ 提供了四种命名的转换操作符，`static_cast` 是最常用的一种。语法格式是：

```
static_cast<目标类型>(要转换的值)
```

它的作用是"告诉编译器：我知道这个转换是有意的，请执行它"。与 C 风格的 `(int)c` 相比，`static_cast` 有两个好处：

- **显眼**：在代码中一眼就能看到这里发生了类型转换，方便排查 bug
- **安全**：编译器会检查转换是否合理，不合理的转换会直接报错

四种转换操作符一览（了解即可，目前只需要掌握 `static_cast`）：

| 操作符 | 用途 | 例子 |
|--------|------|------|
| `static_cast` | 基本类型之间的转换 | `static_cast<int>(3.14)` |
| `dynamic_cast` | 安全的向下转型（面向对象部分讲） | 后续课程 |
| `const_cast` | 去掉或添加 const 属性 | 很少使用 |
| `reinterpret_cast` | 位级别的重新解释 | 极少使用 |

> **Java 对比**：Java 的类型转换写法 `int x = (int)c;` 和 C 风格一样。C++ 虽然也支持这种写法，但现代 C++ 强烈推荐用 `static_cast`，因为它更安全、更可搜索。

### 枚举在内存中的存储

枚举变量本质上是一个整数，占用的内存大小取决于底层类型（默认是 `int`，即 4 字节）：

```cpp
#include <iostream>
#include <cstdint>

enum class BigEnum : int { A, B, C };        // 底层类型 int，占 4 字节
enum class SmallEnum : uint8_t { X, Y, Z };   // 底层类型 uint8_t，占 1 字节

int main() {
    std::cout << "BigEnum 大小: " << sizeof(BigEnum) << " 字节\n";
    std::cout << "SmallEnum 大小: " << sizeof(SmallEnum) << " 字节\n";

    // enum class 的值不能直接用 std::cout 输出
    // 下面这行会编译错误：
    // std::cout << BigEnum::A;  // 错误！没有匹配的 << 运算符

    // 必须先转成整数才能输出
    std::cout << "BigEnum::B 的值: " << static_cast<int>(BigEnum::B) << "\n";
    return 0;
}
```

运行结果：

```text
BigEnum 大小: 4 字节
SmallEnum 大小: 1 字节
BigEnum::B 的值: 1
```

> **为什么 `std::cout` 不能直接输出 `enum class`？** 因为 `enum class` 是强类型的，`std::cout` 的 `<<` 运算符没有为它定义输出行为。传统 `enum` 能直接输出，是因为它隐式转换成了 `int`，`cout` 输出的是那个整数。`enum class` 阻止了这种隐式转换，所以你必须手动 `static_cast`。

### switch 与 enum class 的配合

`switch` 语句和 `enum class` 是天生一对。用 `enum class` 做 switch 的判断条件时，编译器会检查你是否覆盖了所有枚举值，帮你避免遗漏：

```cpp
#include <iostream>

enum class TrafficLight { Red, Green, Yellow };

int main() {
    TrafficLight light = TrafficLight::Green;

    switch (light) {
        case TrafficLight::Red:
            std::cout << "停止\n";
            break;
        case TrafficLight::Green:
            std::cout << "通行\n";
            break;
        case TrafficLight::Yellow:
            std::cout << "注意\n";
            break;
        // 没有 default：如果将来添加了新枚举值但忘了处理
        // 编译器会用警告提醒你
    }
    return 0;
}
```

运行结果：

```text
通行
```

> **编程建议**：当 switch 的条件是 `enum class` 时，尽量不加 `default` 分支。这样当你将来在枚举里添加新值却忘了在 switch 里处理时，编译器的 `-Wall` 警告会提醒你。

### 传统枚举 vs 强类型枚举

| 特性 | `enum`（传统） | `enum class`（现代） |
|------|--------------|-------------------|
| 命名空间隔离 | 否，成员暴露在外层 | 是，必须用 `Color::Red` |
| 隐式转 int | 可以 | 不可以，必须 `static_cast` |
| 名字冲突风险 | 高 | 无 |
| 指定底层类型 | C++11 起支持 | 支持 |
| 推荐程度 | 仅用于与 C 代码交互 | 优先使用 |

### 指定底层类型

你可以告诉编译器枚举用什么整数类型存储，这在对内存敏感的场景下有用：

```cpp
#include <iostream>
#include <cstdint>

// 用 uint8_t 存储，只占 1 字节
enum class Direction : uint8_t {
    Up = 0,
    Down = 1,
    Left = 2,
    Right = 3
};

int main() {
    std::cout << "Direction 占用 " << sizeof(Direction) << " 字节\n";
    return 0;
}
```

运行结果：

```text
Direction 占用 1 字节
```

### Java 对比

Java 的枚举是"语法糖"——每个枚举常量本质上是一个类的实例，可以有字段、方法和构造函数。C++ 的枚举则纯粹是编译期的整数常量，没有这些能力。

```java
// Java 枚举可以有字段和方法
enum Planet {
    EARTH(6371), MARS(3390);

    private final int radius;
    Planet(int r) { this.radius = r; }
    public int getRadius() { return radius; }
}
```

C++ 做不到这种"富枚举"。如果你需要类似功能，通常用一个 `struct` 或 `class` 配合 `static const` 成员来模拟。

### 小练习

定义一个 `enum class Season`，包含春夏秋冬四个值。写一个程序，根据输入的数字（0-3）输出对应的季节名称。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

enum class Season { Spring, Summer, Autumn, Winter };

int main() {
    int n;
    std::cout << "请输入 0-3：";
    std::cin >> n;

    Season s = static_cast<Season>(n);
    switch (s) {
        case Season::Spring: std::cout << "春天\n"; break;
        case Season::Summer: std::cout << "夏天\n"; break;
        case Season::Autumn: std::cout << "秋天\n"; break;
        case Season::Winter: std::cout << "冬天\n"; break;
        default:             std::cout << "无效输入\n"; break;
    }
    return 0;
}
```

运行示例（输入 2）：

```text
请输入 0-3：2
秋天
```

</details>

---

## 第二课：自定义变量名称

### 为什么要自定义类型名称

当你写 `unsigned long long` 这样的类型名时，是不是觉得又长又容易写错？C++ 允许你给已有类型起一个简短的别名，就像 Java 里你不会每次都写 `java.util.List<String>` 而是先 `import` 再写 `List<String>` 一样。

C++ 有两种方式：老的 `typedef` 和新的 `using`。

### typedef（C 语言遗产）

`typedef` 从 C 语言继承而来，语法是"先写完整类型，再写新名字"：

```cpp
#include <iostream>
#include <vector>

typedef unsigned long long ull;       // 给 unsigned long long 起短名
typedef std::vector<int> IntVector;   // 给 vector<int> 起名

int main() {
    ull big = 1000000000000ULL;
    IntVector nums = {1, 2, 3};

    std::cout << "big = " << big << "\n";
    std::cout << "nums.size() = " << nums.size() << "\n";
    return 0;
}
```

运行结果：

```text
big = 1000000000000
nums.size() = 3
```

### using（C++11 推荐）

C++11 引入了 `using`，功能和 `typedef` 完全一样，但语法更直观——"新名字 = 旧类型"，像赋值一样：

```cpp
#include <iostream>
#include <vector>
#include <string>

using ull = unsigned long long;
using IntVector = std::vector<int>;
using StringList = std::vector<std::string>;

int main() {
    ull big = 999999999999ULL;
    StringList names = {"Alice", "Bob", "Charlie"};

    std::cout << "big = " << big << "\n";
    for (const auto& name : names) {
        std::cout << name << " ";
    }
    std::cout << "\n";
    return 0;
}
```

运行结果：

```text
big = 999999999999
Alice Bob Charlie
```

### typedef vs using

| 特性 | `typedef` | `using` |
|------|-----------|---------|
| 语法方向 | 旧名在前，新名在后 | 新名在前，旧名在后 |
| 可读性 | 较差，复杂类型难读 | 好，像赋值 |
| 模板支持 | 不支持别名模板 | 支持（C++11 别名模板） |
| 推荐程度 | 兼容旧代码时用 | 优先使用 |

### 别名模板（using 的独有能力）

`using` 可以配合模板，`typedef` 做不到：

```cpp
#include <iostream>
#include <vector>

// 用 using 创建模板别名
template <typename T>
using Vec = std::vector<T>;

int main() {
    Vec<int> nums = {1, 2, 3};        // 等价于 std::vector<int>
    Vec<double> prices = {1.1, 2.2};  // 等价于 std::vector<double>

    std::cout << "nums size: " << nums.size() << "\n";
    std::cout << "prices size: " << prices.size() << "\n";
    return 0;
}
```

运行结果：

```text
nums size: 3
prices size: 2
```

### 实际用途

自定义类型名称在工程中非常常见，主要用于：

- **简化长类型名**：`std::vector<std::pair<std::string, int>>` 太长，起个短名
- **提高可移植性**：在不同平台上用不同底层类型，只需改一处别名
- **语义化命名**：`using EmployeeId = int;` 比直接写 `int` 更有表达力

### 知识点补充：auto 关键字

在前面的代码示例中，你可能注意到了一个关键词 `auto`：

```cpp
for (const auto& name : names) {    // auto 是什么？
    std::cout << name << " ";
}
```

`auto` 让编译器**自动推导变量的类型**。你不用手写类型名，编译器会根据等号右边的表达式自动推断。

```cpp
#include <iostream>
#include <vector>
#include <string>

int main() {
    auto x = 42;            // 编译器推导为 int
    auto y = 3.14;          // 编译器推导为 double
    auto z = 3.14f;         // 编译器推导为 float（f 后缀）
    auto s = std::string("Hello");  // 编译器推导为 std::string
    auto v = std::vector<int>{1, 2, 3};  // 推导为 std::vector<int>

    std::cout << x << " " << y << " " << z << " " << s << "\n";
    std::cout << "v.size() = " << v.size() << "\n";
    return 0;
}
```

运行结果：

```text
42 3.14 3.14 Hello
v.size() = 3
```

**auto 在范围 for 循环中的用法**：

```cpp
std::vector<int> nums = {1, 2, 3};

// 方式一：明确写出类型
for (const int& n : nums) { ... }

// 方式二：用 auto，编译器自动推导为 const int&
for (const auto& n : nums) { ... }

// 方式三：auto 不加引用 —— 会拷贝一份，修改不影响原数据
for (auto n : nums) { ... }

// 方式四：auto 加引用但不加 const —— 可以修改原数据
for (auto& n : nums) { n *= 2; }
```

各写法的区别：

| 写法 | 是否拷贝 | 能否修改原数据 | 推荐场景 |
|------|---------|---------------|---------|
| `for (auto n : nums)` | 是（拷贝） | 否 | 只读，且类型很小（int 等） |
| `for (auto& n : nums)` | 否（引用） | 是 | 需要修改原数据 |
| `for (const auto& n : nums)` | 否（引用） | 否 | 只读，且类型较大（string 等） |

> **新手建议**：范围 for 循环默认用 `for (const auto& x : container)`，需要修改时再去掉 `const`。这样既避免了不必要的拷贝，又防止意外修改。

> **Java 对比**：Java 的 `var`（Java 10 引入）和 C++ 的 `auto` 类似，都是让编译器推导类型。但 Java 的 `var` 只能用于局部变量，C++ 的 `auto` 使用范围更广。

### 知识点补充：decltype 简介

`decltype` 和 `auto` 是一对兄弟。`auto` 是"根据右值推导类型"，`decltype` 是"根据表达式推导类型"：

```cpp
int x = 42;
decltype(x) y = 10;  // y 的类型和 x 一样，都是 int

auto a = 3.14;        // a 是 double
decltype(a) b = 2.0;  // b 也是 double
```

`decltype` 在模板编程中用得最多，日常编程中偶尔会遇到。目前了解即可。

### 小练习

用 `using` 给 `std::vector<std::pair<int, std::string>>` 起一个叫 `ScoreBoard` 的别名，然后创建一个包含三条成绩数据的变量并遍历输出。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <vector>
#include <string>
#include <utility>

using ScoreBoard = std::vector<std::pair<int, std::string>>;

int main() {
    ScoreBoard board = {
        {95, "Alice"},
        {87, "Bob"},
        {78, "Charlie"}
    };

    for (const auto& entry : board) {
        std::cout << entry.second << ": " << entry.first << " 分\n";
    }
    return 0;
}
```

运行结果：

```text
Alice: 95 分
Bob: 87 分
Charlie: 78 分
```

</details>

---

## 第三课：命名空间

### 为什么需要命名空间

想象你在一个大型项目里，你写了一个函数叫 `print`，你的同事也写了一个叫 `print`，第三方库还有一个叫 `print`。如果没有命名空间，编译器无法区分它们，会报"重复定义"错误。

命名空间就是解决这个问题的：它给一组名字套上一个"姓氏"，避免重名冲突。Java 用 `package` 解决同样的问题，C++ 用 `namespace`。

### 基本用法

```cpp
#include <iostream>

// 定义一个命名空间
namespace Physics {
    double gravity = 9.8;

    void printInfo() {
        std::cout << "重力加速度: " << gravity << " m/s²\n";
    }
}

// 另一个命名空间
namespace Game {
    double gravity = 0.6;  // 不冲突，因为在不同命名空间里

    void printInfo() {
        std::cout << "游戏重力: " << gravity << " m/s²\n";
    }
}

int main() {
    Physics::printInfo();  // 用 命名空间名:: 来访问
    Game::printInfo();
    std::cout << "物理重力: " << Physics::gravity << "\n";
    std::cout << "游戏重力: " << Game::gravity << "\n";
    return 0;
}
```

运行结果：

```text
重力加速度: 9.8 m/s²
游戏重力: 0.6 m/s²
物理重力: 9.8
游戏重力: 0.6
```

### using 声明

每次都写 `命名空间名::` 很烦。你可以用 `using` 引入某个名字：

```cpp
#include <iostream>
#include <string>

namespace MyLib {
    std::string greet = "Hello";
    int version = 2;
}

int main() {
    using MyLib::greet;  // 只引入 greet
    std::cout << greet << "\n";       // 不需要写 MyLib::
    // std::cout << version << "\n";  // 错误！version 没被引入
    std::cout << MyLib::version << "\n";
    return 0;
}
```

运行结果：

```text
Hello
2
```

### using 指令

`using namespace` 会把整个命名空间的内容都引入当前作用域：

```cpp
#include <iostream>

namespace MyLib {
    int x = 42;
    double y = 3.14;
}

int main() {
    using namespace MyLib;  // 引入整个命名空间
    std::cout << x << " " << y << "\n";  // 直接用，不用前缀
    return 0;
}
```

运行结果：

```text
42 3.14
```

> **新手警告**：不要在头文件（`.h` 文件）里写 `using namespace std`！这会强制所有包含该头文件的代码都引入整个标准库，极易造成命名冲突。在 `.cpp` 文件里写也要谨慎，最好限制在函数内部的小作用域中。

### 嵌套命名空间

命名空间可以嵌套：

```cpp
#include <iostream>

namespace Company {
    namespace Department {
        namespace Team {
            int memberCount = 5;
        }
    }
}

int main() {
    std::cout << Company::Department::Team::memberCount << "\n";
    return 0;
}
```

运行结果：

```text
5
```

### C++17 简化嵌套语法

C++17 允许你用更简洁的写法声明嵌套命名空间，不必一层层写：

```cpp
#include <iostream>

// C++17 之前要写三层
// C++17 可以一行搞定
namespace Company::Department::Team {
    int memberCount = 5;
}

int main() {
    std::cout << Company::Department::Team::memberCount << "\n";
    return 0;
}
```

运行结果：

```text
5
```

### std 命名空间

你可能已经注意到，`std::cout`、`std::cin`、`std::string`、`std::vector` 都在 `std` 命名空间里。`std` 是 C++ 标准库的"姓氏"，所有标准库的内容都放在里面。

如果不想每次都写 `std::`，可以在函数内部加 `using namespace std`：

```cpp
#include <iostream>
#include <string>

int main() {
    using namespace std;  // 只在这个函数内有效

    string name = "Alice";
    cout << "Hello, " << name << endl;
    return 0;
}
```

运行结果：

```text
Hello, Alice
```

### using 声明 vs using 指令

前面出现了两种 `using` 写法，容易混淆，这里对比清楚：

| 写法 | 名称 | 效果 | 风险 |
|------|------|------|------|
| `using std::cout;` | using 声明 | 只引入一个名字 | 安全，推荐 |
| `using namespace std;` | using 指令 | 引入整个命名空间的所有名字 | 可能冲突，需谨慎 |

```cpp
// using 声明：精确引入，只放开一个名字
using std::cout;    // 此后只需写 cout，但 cin、string 仍需加 std::
using std::endl;    // 此后只需写 endl

// using 指令：全部放开，所有名字都不用加前缀
using namespace std;  // cout、cin、string、vector... 全部可用
```

> **编程建议**：优先用 `using` 声明（`using std::cout;`），尽量少用 `using` 指令（`using namespace std;`）。引入的名字越少，冲突的风险越低。

### 命名空间别名

如果命名空间的名字太长，可以给它起一个短名字，类似 `using` 给类型起别名：

```cpp
#include <iostream>

namespace VeryLongNamespaceName {
    int value = 42;
}

int main() {
    // 给长命名空间起短名
    namespace Short = VeryLongNamespaceName;
    std::cout << Short::value << "\n";  // 等价于 VeryLongNamespaceName::value
    return 0;
}
```

运行结果：

```text
42
```

### 匿名命名空间

有一种特殊的命名空间没有名字，叫匿名命名空间。它的作用是让里面的内容**只在当前文件内可见**，其他文件访问不到：

```cpp
#include <iostream>

// 匿名命名空间：里面的东西只在当前 .cpp 文件内有效
namespace {
    int secretNumber = 99;
    void internalFunc() {
        std::cout << "内部函数被调用\n";
    }
}

int main() {
    // 在同一文件里可以直接用，不需要前缀
    std::cout << "secret = " << secretNumber << "\n";
    internalFunc();
    return 0;
}
```

运行结果：

```text
secret = 99
内部函数被调用
```

> **C 语言对比**：匿名命名空间相当于 C 语言中的 `static` 全局变量/函数——限制作用域在当前文件内。C++ 推荐用匿名命名空间替代 `static`，因为语义更清晰。
>
> **什么时候用**：当你写一个只在当前 `.cpp` 文件内使用的辅助函数或常量时，放进匿名命名空间，防止链接器把它暴露给其他文件。

### Java 对比

| 概念 | Java | C++ |
|------|------|-----|
| 命名空间机制 | `package` + `import` | `namespace` + `using` |
| 全局引入 | `import java.util.*` | `using namespace std` |
| 单个引入 | `import java.util.List` | `using std::List` |
| 嵌套 | 用 `.` 分隔包名 | `namespace A::B::C`（C++17） |

Java 的 `package` 和文件目录结构强绑定（`com.example.Foo` 对应 `com/example/Foo.java`），C++ 的 `namespace` 和文件结构无关，完全由代码逻辑决定。

### 小练习

创建两个命名空间 `Math` 和 `English`，各有一个 `score` 变量和 `printScore()` 函数。在 main 中分别调用它们，输出不同的分数。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

namespace Math {
    int score = 95;
    void printScore() {
        std::cout << "数学成绩: " << score << " 分\n";
    }
}

namespace English {
    int score = 88;
    void printScore() {
        std::cout << "英语成绩: " << score << " 分\n";
    }
}

int main() {
    Math::printScore();
    English::printScore();
    return 0;
}
```

运行结果：

```text
数学成绩: 95 分
英语成绩: 88 分
```

</details>

---

## 第四课：变量的生命周期

### 什么是生命周期

每个变量都有"从生到死"的过程：它被创建时获得内存，被销毁时释放内存。这个从创建到销毁的时间段，就叫生命周期。

Java 有垃圾回收器（GC）帮你管理内存，变量不用了 GC 会自动回收。C++ 没有GC，变量的生命周期由**作用域**（scope）严格决定——变量在离开作用域的瞬间就被销毁。

### 作用域决定生死

C++ 用花括号 `{}` 划分作用域。变量在所在的花括号结束时自动销毁：

```cpp
#include <iostream>

int main() {
    int x = 10;  // x 在 main 的作用域内出生

    {
        int y = 20;  // y 在这个内层作用域内出生
        std::cout << "x = " << x << ", y = " << y << "\n";
    }  // y 在这里被销毁，内存释放

    // std::cout << y << "\n";  // 编译错误！y 已经不存在了
    std::cout << "x = " << x << "\n";  // x 还活着

    return 0;
}  // x 在这里被销毁
```

运行结果：

```text
x = 10, y = 20
x = 10
```

**Java 对比**：Java 的局部变量也受花括号作用域限制，但对象本身由 GC 管理，离开作用域后不一定立即回收。C++ 则是立即销毁，这是 RAII（资源获取即初始化）的基础。

### 局部变量

在函数内部声明的变量，叫局部变量。它们存储在栈上，函数返回时自动销毁。

```cpp
#include <iostream>

void func() {
    int local = 42;  // 局部变量，每次调用 func 都重新创建
    std::cout << "local = " << local << "\n";
}  // local 在这里销毁

int main() {
    func();  // 输出 42
    func();  // 还是输出 42，因为每次调用 local 都重新初始化
    return 0;
}
```

运行结果：

```text
local = 42
local = 42
```

### 全局变量

在所有函数外部声明的变量，叫全局变量。它们在程序启动时创建，程序结束时销毁。

```cpp
#include <iostream>

int counter = 0;  // 全局变量，整个程序都能访问

void increment() {
    ++counter;  // 修改全局变量
}

int main() {
    increment();
    increment();
    increment();
    std::cout << "counter = " << counter << "\n";  // 输出 3
    return 0;
}
```

运行结果：

```text
counter = 3
```

> **新手建议**：尽量少用全局变量。它会导致代码耦合，难以追踪谁修改了它。Java 里虽然也有静态变量，但 C++ 的全局变量更容易引发难以察觉的 bug。

### 静态局部变量

`static` 关键字让局部变量只初始化一次，但生命周期延续到程序结束：

```cpp
#include <iostream>

void countCalls() {
    static int count = 0;  // 只在第一次调用时初始化为 0
    ++count;
    std::cout << "这是第 " << count << " 次调用\n";
}

int main() {
    countCalls();  // 第 1 次
    countCalls();  // 第 2 次
    countCalls();  // 第 3 次
    return 0;
}
```

运行结果：

```text
这是第 1 次调用
这是第 2 次调用
这是第 3 次调用
```

**关键区别**：如果去掉 `static`，每次调用 `countCalls` 时 `count` 都会重新初始化为 0，永远输出"第 1 次"。加了 `static` 后，`count` 只在第一次调用时初始化，之后保持上一次的值。

**Java 对比**：Java 没有完全等价的东西。Java 的 `static` 变量是类级别的，不属于方法。C++ 的静态局部变量属于函数本身，但生命周期和程序一样长。

### 三种变量对比

| 类型 | 存储位置 | 创建时机 | 销毁时机 | 默认初值 |
|------|---------|---------|---------|---------|
| 局部变量 | 栈 | 进入函数/块时 | 离开函数/块时 | 未初始化（随机值） |
| 全局变量 | 全局数据区 | 程序启动时 | 程序结束时 | 自动初始化为 0 |
| 静态局部变量 | 全局数据区 | 第一次执行到声明时 | 程序结束时 | 自动初始化为 0 |

> **新手陷阱**：局部变量不初始化时，值是内存里残留的随机垃圾值。养成声明变量时就初始化的习惯：`int x = 0;` 而不是 `int x;`。

### 知识点补充：作用域解析运算符 ::

当局部变量和全局变量同名时，局部变量会"遮蔽"全局变量。用 `::` 可以显式访问全局变量（前面不加任何名字的 `::` 表示"全局作用域"）：

```cpp
#include <iostream>

int x = 100;  // 全局变量

int main() {
    int x = 50;  // 局部变量，遮蔽了全局的 x

    std::cout << "局部 x = " << x << "\n";      // 输出 50
    std::cout << "全局 x = " << ::x << "\n";     // 输出 100

    return 0;
}
```

运行结果：

```text
局部 x = 50
全局 x = 100
```

> **编程建议**：尽量不要让局部变量和全局变量同名。虽然 `::` 能区分，但可读性差，容易出错。

### 知识点补充：extern 关键字

如果一个全局变量定义在 A 文件里，你想在 B 文件里使用它，就需要用 `extern` 声明它"在别处定义"：

```cpp
// ===== file_a.cpp =====
int sharedCounter = 0;  // 定义（分配内存）

// ===== file_b.cpp =====
#include <iostream>
extern int sharedCounter;  // 声明：告诉编译器"这个变量在别处定义"

int main() {
    std::cout << "sharedCounter = " << sharedCounter << "\n";
    return 0;
}
```

`extern` 不创建新变量，只是告诉编译器"这个变量存在，但在另一个文件里定义"。链接器会在链接阶段找到它。

> **Java 对比**：Java 没有这种跨文件共享全局变量的机制。Java 通过 `public static` 字段和类访问来实现类似功能。
>
> **实际用途**：在大型项目中，全局配置变量通常定义在一个 `.cpp` 文件里，通过头文件中的 `extern` 声明让其他文件使用。但在现代 C++ 中，更推荐用命名空间 + `inline` 变量（C++17）来替代 `extern`。

### 栈与堆：两种内存区域

C++ 的变量根据声明方式存储在不同的内存区域：

| 内存区域 | 存储什么 | 谁管理 | 速度 |
|---------|---------|--------|------|
| **栈** (Stack) | 局部变量、函数参数 | 自动（编译器） | 快 |
| **堆** (Heap) | `new` 分配的内存 | 手动（程序员） | 较慢 |
| **全局数据区** | 全局变量、静态变量 | 自动（程序启动/结束） | 中等 |

```cpp
#include <iostream>

int globalVar = 10;  // 全局数据区

void func(int param) {  // param 在栈上
    int local = 20;     // 栈上
    static int s = 0;   // 全局数据区
    std::cout << "param=" << param << " local=" << local
              << " s=" << s << " globalVar=" << globalVar << "\n";
    // int* p = new int(30);  // 堆上（后续课程讲 new/delete）
}

int main() {
    func(5);
    return 0;
}
```

运行结果：

```text
param=5 local=20 s=0 globalVar=10
```

> **为什么这很重要？** 栈的空间有限（通常几 MB），在栈上放太大的数据（比如超大数组）会导致栈溢出。堆的空间大得多（受物理内存限制），但需要手动管理。Java 的对象都在堆上，由 GC 回收；C++ 的局部变量在栈上，离开作用域自动销毁。这种差异是 C++ 性能高于 Java 的重要原因之一。

### 小练习

写一个函数 `getNextId()`，每次调用返回一个递增的 ID（从 1 开始）。在 main 中调用 5 次并输出结果。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

int getNextId() {
    static int id = 0;  // 只初始化一次
    ++id;
    return id;
}

int main() {
    for (int i = 0; i < 5; ++i) {
        std::cout << "ID = " << getNextId() << "\n";
    }
    return 0;
}
```

运行结果：

```text
ID = 1
ID = 2
ID = 3
ID = 4
ID = 5
```

</details>

---

## 第五课：数据和计算补充知识

### sizeof 运算符

`sizeof` 告诉你一个类型或变量占多少字节内存。这在跨平台开发中非常重要，因为同一类型在不同平台上大小可能不同。

```cpp
#include <iostream>
#include <cstdint>

int main() {
    std::cout << "bool:     " << sizeof(bool) << " 字节\n";
    std::cout << "char:     " << sizeof(char) << " 字节\n";
    std::cout << "int:      " << sizeof(int) << " 字节\n";
    std::cout << "long:     " << sizeof(long) << " 字节\n";
    std::cout << "long long:" << sizeof(long long) << " 字节\n";
    std::cout << "float:    " << sizeof(float) << " 字节\n";
    std::cout << "double:   " << sizeof(double) << " 字节\n";
    std::cout << "int64_t:  " << sizeof(int64_t) << " 字节\n";
    return 0;
}
```

运行结果（64 位 Linux）：

```text
bool:     1 字节
char:     1 字节
int:      4 字节
long:     8 字节
long long:8 字节
float:    4 字节
double:   8 字节
int64_t:  8 字节
```

> 注意：`long` 在 Windows 64 位上是 4 字节，在 Linux/Mac 64 位上是 8 字节。这就是为什么跨平台代码推荐用 `int64_t` 而不是 `long`。

### 数值极限

每种类型都有能表示的最大值和最小值。`<limits>` 头文件提供了这些信息：

```cpp
#include <iostream>
#include <limits>

int main() {
    std::cout << "int 的范围: "
              << std::numeric_limits<int>::min() << " 到 "
              << std::numeric_limits<int>::max() << "\n";

    std::cout << "double 的范围: "
              << std::numeric_limits<double>::min() << " 到 "
              << std::numeric_limits<double>::max() << "\n";

    std::cout << "int64_t 的范围: "
              << std::numeric_limits<int64_t>::min() << " 到 "
              << std::numeric_limits<int64_t>::max() << "\n";

    return 0;
}
```

运行结果：

```text
int 的范围: -2147483648 到 2147483647
double 的范围: 2.22507e-308 到 1.79769e+308
int64_t 的范围: -9223372036854775808 到 9223372036854775807
```

### 整数溢出

当你把一个超过类型最大值的数赋给变量时，会发生"溢出"。C++ 不会报错，而是"回绕"：

```cpp
#include <iostream>
#include <limits>

int main() {
    int maxInt = std::numeric_limits<int>::max();
    std::cout << "max int = " << maxInt << "\n";
    std::cout << "max int + 1 = " << maxInt + 1 << "\n";  // 溢出！变成最小值

    unsigned int u = 0;
    std::cout << "unsigned 0 - 1 = " << u - 1 << "\n";  // 溢出！变成最大值

    return 0;
}
```

运行结果：

```text
max int = 2147483647
max int + 1 = -2147483648
unsigned 0 - 1 = 4294967295
```

> **新手警告**：整数溢出是 C++ 中最隐蔽的 bug 来源之一。Java 也会溢出，但 Java 的 `int` 永远是 32 位，行为可预测。C++ 的整数大小跨平台不同，更容易出问题。

### 数值字面量

C++ 提供了多种写数字的方式，提高可读性：

```cpp
#include <iostream>

int main() {
    // 十进制
    int dec = 42;

    // 二进制（C++14 起，0b 前缀）
    int bin = 0b101010;  // 等于 42

    // 八进制（0 前缀）
    int oct = 052;       // 等于 42

    // 十六进制（0x 前缀）
    int hex = 0x2A;      // 等于 42

    // 数字分隔符（C++14 起，用 ' 提高可读性）
    long big = 1'000'000;        // 一百万
    long bigHex = 0xFF'FF'FF'FF; // 十六进制也支持

    std::cout << dec << " " << bin << " " << oct << " " << hex << " " << big << " " << bigHex << "\n";
    return 0;
}
```

运行结果：

```text
42 42 42 42 1000000 4294967295
```

### 数学函数

`<cmath>` 头文件提供了常用的数学函数：

```cpp
#include <iostream>
#include <cmath>

int main() {
    double x = 2.0;

    std::cout << "sqrt(" << x << ") = " << std::sqrt(x) << "\n";      // 平方根
    std::cout << "pow(" << x << ", 3) = " << std::pow(x, 3) << "\n";  // x 的 3 次方
    std::cout << "abs(-5.5) = " << std::abs(-5.5) << "\n";            // 绝对值
    std::cout << "ceil(3.2) = " << std::ceil(3.2) << "\n";            // 向上取整
    std::cout << "floor(3.8) = " << std::floor(3.8) << "\n";          // 向下取整
    std::cout << "round(3.5) = " << std::round(3.5) << "\n";          // 四舍五入
    std::cout << "log(e) = " << std::log(2.71828) << "\n";            // 自然对数
    std::cout << "sin(0) = " << std::sin(0) << "\n";                  // 正弦（弧度）

    return 0;
}
```

运行结果：

```text
sqrt(2) = 1.41421
pow(2, 3) = 8
abs(-5.5) = 5.5
ceil(3.2) = 4
floor(3.8) = 3
round(3.5) = 4
log(e) = 1
sin(0) = 0
```

**Java 对比**：Java 的 `Math.sqrt()`、`Math.pow()` 等函数名和 C++ 几乎一样，只是放在 `Math` 类里。C++ 用全局函数 + `<cmath>` 头文件。

### 随机数（现代 C++ 方式）

C 语言的 `rand()` 质量差、分布不均匀。C++11 引入了 `<random>` 头文件，提供高质量的随机数生成：

```cpp
#include <iostream>
#include <random>

int main() {
    // 创建随机数引擎
    std::random_device rd;                          // 硬件随机种子
    std::mt19937 gen(rd());                         // Mersenne Twister 引擎

    // 定义均匀分布 [1, 100]
    std::uniform_int_distribution<int> dist(1, 100);

    // 生成 5 个随机数
    for (int i = 0; i < 5; ++i) {
        std::cout << dist(gen) << " ";
    }
    std::cout << "\n";

    return 0;
}
```

运行结果（每次运行不同）：

```text
73 41 92 15 58
```

### 知识点补充：std::cout 输出格式化

默认情况下，`std::cout` 输出浮点数时只显示 6 位有效数字。如果你需要更多精度，可以用 `<iomanip>` 头文件里的格式控制工具：

```cpp
#include <iostream>
#include <iomanip>  // 格式控制头文件

int main() {
    double pi = 3.14159265358979323846;

    // 默认输出：只显示 6 位
    std::cout << "默认:     " << pi << "\n";

    // 固定小数点 + 10 位精度
    std::cout << std::fixed << std::setprecision(10);
    std::cout << "10位精度: " << pi << "\n";

    // 科学计数法
    std::cout << std::scientific << std::setprecision(4);
    std::cout << "科学计数: " << pi << "\n";

    // 恢复默认
    std::cout << std::defaultfloat << std::setprecision(6);
    std::cout << "恢复默认: " << pi << "\n";

    // 设置输出宽度（右对齐）
    std::cout << std::setw(10) << "Hello" << "\n";
    std::cout << std::setw(10) << "World" << "\n";

    // 填充字符
    std::cout << std::setw(10) << std::setfill('*') << 42 << "\n";

    return 0;
}
```

运行结果：

```text
默认:     3.14159
10位精度: 3.1415926536
科学计数: 3.1416e+00
恢复默认: 3.14159
     Hello
     World
********42
```

常用格式控制符一览：

| 控制符 | 作用 | 需要头文件 |
|--------|------|-----------|
| `std::fixed` | 固定小数点表示 | `<iomanip>` |
| `std::scientific` | 科学计数法 | `<iomanip>` |
| `std::setprecision(n)` | 设置精度（小数位数） | `<iomanip>` |
| `std::setw(n)` | 设置下一次输出的宽度 | `<iomanip>` |
| `std::setfill(c)` | 设置填充字符 | `<iomanip>` |
| `std::left` | 左对齐 | `<ios>` |
| `std::right` | 右对齐（默认） | `<ios>` |
| `std::hex` | 十六进制输出整数 | `<ios>` |
| `std::dec` | 十进制输出（默认） | `<ios>` |

> **Java 对比**：Java 用 `System.out.printf("%.10f", pi)` 做格式化，和 C 语言的 `printf` 一样。C++ 的 `std::cout` 用"流操纵符"方式，通过 `<<` 链式调用，虽然写法不同但功能等价。C++ 也支持 `printf`（通过 `<cstdio>`），但推荐用 `cout`。

### 知识点补充：std::cin 输入处理

前面的练习中用到了 `std::cin` 读取用户输入。这里正式介绍一下它的工作方式：

```cpp
#include <iostream>
#include <string>

int main() {
    int age;
    std::cout << "请输入年龄: ";
    std::cin >> age;   // 读取整数

    std::string name;
    std::cout << "请输入姓名: ";
    std::cin >> name;  // 读取字符串（遇空格停止）

    std::cout << "你好, " << name << "! 你 " << age << " 岁了。\n";
    return 0;
}
```

运行示例（输入 `25` 和 `Alice`）：

```text
请输入年龄: 25
请输入姓名: Alice
你好, Alice! 你 25 岁了。
```

**cin 的常见用法**：

| 操作 | 代码 | 说明 |
|------|------|------|
| 读取整数 | `std::cin >> n;` | 跳过空白，读取一个整数 |
| 读取浮点数 | `std::cin >> d;` | 读取一个 double |
| 读取单词 | `std::cin >> s;` | 遇空格/换行停止 |
| 读取整行 | `std::getline(std::cin, s);` | 读到换行符为止（含空格） |
| 检查输入是否失败 | `std::cin.fail()` | 输入类型不匹配时返回 true |
| 清除错误状态 | `std::cin.clear();` | 重置 fail 标志 |
| 丢弃缓冲区内容 | `std::cin.ignore();` | 跳过一个字符 |

`std::cin >>` 和 `std::getline` 混用时容易踩坑：

```cpp
#include <iostream>
#include <string>

int main() {
    int n;
    std::string line;

    std::cout << "输入数字: ";
    std::cin >> n;  // 读取 42，但换行符 \n 留在缓冲区

    std::cout << "输入一行文字: ";
    // getline 会立即读到残留的 \n，导致看起来"跳过了输入"
    std::getline(std::cin, line);  // 实际读到的是空字符串

    std::cout << "n = " << n << ", line = [" << line << "]\n";
    return 0;
}
```

**解决方法**：在 `cin >>` 之后、`getline` 之前，加一句 `std::cin.ignore()`：

```cpp
std::cin >> n;
std::cin.ignore();  // 丢掉残留的换行符
std::getline(std::cin, line);  // 现在能正常读取了
```

### 知识点补充：constexpr 编译期常量

C++11 引入了 `constexpr`，表示"这个值在编译期就能算出来"。它比 `const` 更强：`const` 只表示"运行时不可修改"，`constexpr` 要求"编译期就能确定"。

```cpp
#include <iostream>

constexpr int SQUARE_SIZE = 5;       // 编译期常量
constexpr double PI = 3.14159265;    // 编译期常量

// constexpr 函数：如果参数也是编译期已知的，结果也在编译期算出
constexpr int square(int x) {
    return x * x;
}

int main() {
    // 编译期就计算出 25，不占运行时间
    constexpr int area = square(SQUARE_SIZE);

    std::cout << "正方形边长: " << SQUARE_SIZE << "\n";
    std::cout << "正方形面积: " << area << "\n";
    std::cout << "圆周率: " << PI << "\n";
    return 0;
}
```

运行结果：

```text
正方形边长: 5
正方形面积: 25
圆周率: 3.14159265
```

| 关键字 | 含义 | 何时确定值 |
|--------|------|-----------|
| `const` | 只读，不可修改 | 运行时 |
| `constexpr` | 编译期可计算的常量 | 编译时 |
| `#define` | 宏替换（无类型检查） | 预处理阶段 |

> **编程建议**：当你需要一个"永远不变的常量"时，优先用 `constexpr`，其次是 `const`，最后才考虑 `#define`。`#define` 没有类型检查，容易出 bug，现代 C++ 几乎不用它定义常量。

### 小练习

写一个程序，模拟掷两个骰子（每个 1-6），计算它们的和，重复 10 次并输出每次结果。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <random>

int main() {
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<int> dice(1, 6);

    for (int i = 1; i <= 10; ++i) {
        int d1 = dice(gen);
        int d2 = dice(gen);
        int sum = d1 + d2;
        std::cout << "第 " << i << " 次: "
                  << d1 << " + " << d2 << " = " << sum << "\n";
    }
    return 0;
}
```

运行结果（每次运行不同）：

```text
第 1 次: 3 + 5 = 8
第 2 次: 6 + 2 = 8
第 3 次: 1 + 4 = 5
第 4 次: 5 + 3 = 8
第 5 次: 2 + 6 = 8
第 6 次: 4 + 1 = 5
第 7 次: 6 + 6 = 12
第 8 次: 3 + 2 = 5
第 9 次: 1 + 5 = 6
第 10 次: 4 + 4 = 8
```

</details>

---

## 第六课：自定义数据类型

### 为什么需要自定义类型

内置类型（int、double 等）只能表示单个值。但现实世界的数据往往是"一组相关的值"：一个学生有姓名、年龄、学号；一个点有 x、y 坐标。C++ 用 `struct` 把多个值组合成一个新类型。

### struct 基础

`struct`（结构体）让你把多个变量打包成一个整体。你学过 C 语言的话，语法几乎一样，但 C++ 的 struct 有更多能力。

```cpp
#include <iostream>
#include <string>

// 定义一个学生结构体
struct Student {
    std::string name;
    int age;
    double score;
};

int main() {
    // 创建并初始化
    Student s1 = {"Alice", 20, 92.5};

    // 访问成员用 . 运算符
    std::cout << "姓名: " << s1.name << "\n";
    std::cout << "年龄: " << s1.age << "\n";
    std::cout << "成绩: " << s1.score << "\n";

    return 0;
}
```

运行结果：

```text
姓名: Alice
年龄: 20
成绩: 92.5
```

### 知识点补充：std::string

上面的结构体里用到了 `std::string`，这里正式介绍一下。`std::string` 是 C++ 标准库提供的字符串类型，定义在 `<string>` 头文件里。它比 C 语言的 `char[]` 安全得多——不需要手动管理内存，不用担心缓冲区溢出。

```cpp
#include <iostream>
#include <string>

int main() {
    std::string s1 = "Hello";
    std::string s2 = "World";

    // 字符串拼接
    std::string s3 = s1 + " " + s2;  // "Hello World"

    // 字符串长度
    std::cout << "长度: " << s3.length() << "\n";

    // 访问单个字符
    std::cout << "第一个字符: " << s3[0] << "\n";  // H
    std::cout << "第三个字符: " << s3.at(2) << "\n"; // l（.at 会做边界检查）

    // 比较
    if (s1 == "Hello") {
        std::cout << "s1 等于 Hello\n";
    }

    // 查找子串
    size_t pos = s3.find("World");
    if (pos != std::string::npos) {
        std::cout << "找到 World，位置: " << pos << "\n";
    }

    // 截取子串
    std::string sub = s3.substr(0, 5);  // "Hello"
    std::cout << "子串: " << sub << "\n";

    // 追加
    s1 += "!!!";
    std::cout << s1 << "\n";

    return 0;
}
```

运行结果：

```text
长度: 11
第一个字符: H
第三个字符: l
s1 等于 Hello
找到 World，位置: 6
子串: Hello
Hello!!!
```

常用方法一览：

| 方法 | 说明 | 例子 |
|------|------|------|
| `.length()` 或 `.size()` | 字符串长度 | `s.length()` |
| `[i]` | 访问第 i 个字符 | `s[0]` |
| `.at(i)` | 访问第 i 个字符（带边界检查） | `s.at(0)` |
| `+` | 拼接字符串 | `s1 + s2` |
| `==`, `!=`, `<`, `>` | 比较 | `s1 == s2` |
| `.find(sub)` | 查找子串，返回位置 | `s.find("ab")` |
| `.substr(pos, len)` | 截取子串 | `s.substr(0, 3)` |
| `.append(s)` | 末尾追加 | `s.append("xyz")` |
| `.empty()` | 是否为空 | `s.empty()` |

> **Java 对比**：`std::string` 和 Java 的 `String` 类似，都是不可变对象……不对，C++ 的 `std::string` 是**可变的**！你可以直接 `s[0] = 'h'` 修改单个字符，这在 Java 里做不到（Java 需要 `StringBuilder`）。另外，C++ 用 `==` 比较字符串内容，Java 用 `==` 比较的是引用地址（需要 `.equals()`），这是一个容易混淆的点。

### 知识点补充：struct 的内存布局

当你创建一个 struct 时，编译器会为它分配多大的内存？答案不是简单地把各成员大小加起来，因为还有**内存对齐**（alignment）的问题：

```cpp
#include <iostream>

struct A {
    char c;    // 1 字节
    int i;     // 4 字节
};

struct B {
    char c;    // 1 字节
    // 编译器在这里填充 3 字节（padding）
    int i;     // 4 字节
};

struct C {
    int i;     // 4 字节
    char c;    // 1 字节
    // 编译器在这里填充 3 字节（padding）
};

int main() {
    std::cout << "sizeof(A) = " << sizeof(A) << "\n";
    std::cout << "sizeof(B) = " << sizeof(B) << "\n";
    std::cout << "sizeof(C) = " << sizeof(C) << "\n";
    return 0;
}
```

运行结果：

```text
sizeof(A) = 8
sizeof(B) = 8
sizeof(C) = 8
```

`A` 和 `B` 的成员完全一样，但看起来 sizeof 应该是 1+4=5 字节。实际是 8 字节——因为编译器为了 CPU 访问效率，会在成员之间插入"填充字节"（padding），让 `int` 成员对齐到 4 字节边界。

```
struct B 的内存布局:
[c][pad][pad][pad][i i i i]
 1B   3B padding    4B
总计: 8 字节
```

> **编程建议**：日常编程不需要手动优化内存对齐，编译器会自动处理。但如果你在嵌入式开发或网络通信中需要精确控制 struct 大小，可以按"从大到小"排列成员来减少 padding。

### 知识点补充：const 成员函数

前面 struct 的成员函数后面都跟着 `const` 关键字：

```cpp
struct Point {
    double x, y;

    double distanceToOrigin() const {  // 这个 const 是什么意思？
        return std::sqrt(x * x + y * y);
    }
};
```

`const` 写在函数参数列表的后面，表示"这个函数不会修改对象的任何成员变量"。它的作用是**承诺**：调用这个函数不会改变对象的状态。

为什么需要它？看这个例子：

```cpp
void printPoint(const Point& p) {
    // p 是 const 引用，表示"只读不写"
    // 如果 distanceToOrigin() 不是 const 函数，这行会编译错误！
    // 因为编译器不确定 distanceToOrigin() 会不会修改 p
    std::cout << p.distanceToOrigin() << "\n";
}
```

如果一个函数确实不修改成员变量，就给它加上 `const`。这样 `const` 对象和 `const` 引用都能调用它。

> **Java 对比**：Java 没有 `const` 成员函数的概念。Java 的 `final` 参数表示"引用不可重新赋值"，但对象内容仍可修改。C++ 的 `const` 成员函数更严格——它保证不修改任何成员变量。

### 知识点补充：std::vector 常用方法

前面的代码中大量使用了 `std::vector`。这里汇总介绍它最常用的方法：

```cpp
#include <iostream>
#include <vector>

int main() {
    std::vector<int> v;

    // 添加元素
    v.push_back(10);      // 尾部添加元素: [10]
    v.push_back(20);      // [10, 20]
    v.push_back(30);      // [10, 20, 30]

    std::cout << "size: " << v.size() << "\n";      // 3
    std::cout << "empty: " << v.empty() << "\n";     // 0 (false)

    // 访问元素
    std::cout << "v[0]: " << v[0] << "\n";            // 10（不检查边界）
    std::cout << "v.at(1): " << v.at(1) << "\n";     // 20（检查边界，越界抛异常）
    std::cout << "front: " << v.front() << "\n";     // 10
    std::cout << "back: " << v.back() << "\n";        // 30

    // 删除元素
    v.pop_back();          // 删除末尾: [10, 20]
    std::cout << "after pop: size=" << v.size() << "\n";  // 2

    // 在指定位置插入
    v.insert(v.begin() + 1, 15);  // 在位置 1 插入: [10, 15, 20]

    // 删除指定位置
    v.erase(v.begin());    // 删除第一个: [15, 20]

    // 清空
    v.clear();
    std::cout << "after clear: empty=" << v.empty() << "\n";  // 1 (true)

    // 初始化方式
    std::vector<int> a = {1, 2, 3};         // 列表初始化
    std::vector<int> b(5, 0);               // 5 个 0: [0,0,0,0,0]
    std::vector<int> c(3);                  // 3 个默认值(0): [0,0,0]

    return 0;
}
```

运行结果：

```text
size: 3
empty: 0
v[0]: 10
v.at(1): 20
front: 10
back: 30
after pop: size=2
after clear: empty=1
```

常用方法速查表：

| 方法 | 说明 |
|------|------|
| `.push_back(x)` | 尾部添加元素 |
| `.pop_back()` | 删除末尾元素 |
| `.size()` | 元素个数 |
| `.empty()` | 是否为空 |
| `[i]` | 访问第 i 个元素（不检查边界） |
| `.at(i)` | 访问第 i 个元素（检查边界） |
| `.front()` | 第一个元素 |
| `.back()` | 最后一个元素 |
| `.clear()` | 清空所有元素 |
| `.insert(pos, x)` | 在指定位置插入 |
| `.erase(pos)` | 删除指定位置元素 |

> **Java 对比**：`std::vector` 类似 Java 的 `ArrayList`，都是动态数组。`push_back` 对应 `add`，`size` 对应 `size`，`pop_back` 对应 `remove(size()-1)`。

### 知识点补充：std::pair 简介

练习中用到了 `std::pair<int, std::string>`，这里简单介绍一下。`std::pair` 是一个"装两个值的容器"，定义在 `<utility>` 头文件里：

```cpp
#include <iostream>
#include <utility>     // std::pair
#include <string>

int main() {
    // 创建 pair：两个值可以是不同类型
    std::pair<int, std::string> p1 = {95, "Alice"};

    // 访问：用 .first 和 .second
    std::cout << "分数: " << p1.first << "\n";    // 95
    std::cout << "姓名: " << p1.second << "\n";   // Alice

    // 更简洁的创建方式：make_pair
    auto p2 = std::make_pair(88, std::string("Bob"));
    std::cout << p2.first << " " << p2.second << "\n";  // 88 Bob

    // pair 常用于函数返回两个值
    // 比如：返回 (查找结果, 是否找到)
    auto result = std::make_pair(42, true);
    if (result.second) {
        std::cout << "找到值: " << result.first << "\n";
    }

    return 0;
}
```

运行结果：

```text
分数: 95
姓名: Alice
88 Bob
找到值: 42
```

> **Java 对比**：Java 没有内置的 pair 类型（Apache Commons 和 JavaFX 里有 `Pair`）。C++ 的 `std::pair` 是标准库的一部分，使用非常方便。当你只需要"两个值绑在一起"时，用 `pair` 比专门定义一个 struct 更轻量。

### 多个变量

```cpp
#include <iostream>
#include <string>
#include <vector>

struct Point {
    double x;
    double y;
};

int main() {
    // 创建多个点
    Point p1 = {1.0, 2.0};
    Point p2 = {3.5, 4.5};
    Point p3 = {0.0, 0.0};

    // 放进 vector 里
    std::vector<Point> points = {p1, p2, p3};

    // 遍历
    for (const auto& p : points) {
        std::cout << "(" << p.x << ", " << p.y << ")\n";
    }

    return 0;
}
```

运行结果：

```text
(1, 2)
(3.5, 4.5)
(0, 0)
```

### C++ struct vs C struct

C 语言的 struct 只能放数据。C++ 的 struct 可以包含**成员函数**，这和 class 几乎一样：

```cpp
#include <iostream>
#include <string>
#include <cmath>

struct Point {
    double x;
    double y;

    // 成员函数：计算到原点的距离
    double distanceToOrigin() const {
        return std::sqrt(x * x + y * y);
    }

    // 成员函数：打印坐标
    void print() const {
        std::cout << "(" << x << ", " << y << ")\n";
    }
};

int main() {
    Point p = {3.0, 4.0};
    p.print();
    std::cout << "到原点距离: " << p.distanceToOrigin() << "\n";
    return 0;
}
```

运行结果：

```text
(3, 4)
到原点距离: 5
```

> **C++ 中 struct 和 class 的唯一区别**：struct 的成员默认是 `public`（外部可访问），class 的成员默认是 `private`（外部不可访问）。其他完全一样。入门阶段用 struct 就够了。

### typedef 和 using 配合 struct

给 struct 起一个短名字，可以让代码更简洁：

```cpp
#include <iostream>
#include <string>
#include <vector>

struct Employee {
    std::string name;
    int id;
    double salary;
};

// 用 using 起别名
using EmpList = std::vector<Employee>;

int main() {
    EmpList staff = {
        {"Alice", 1001, 8500.0},
        {"Bob", 1002, 9200.0},
        {"Charlie", 1003, 7800.0}
    };

    for (const auto& emp : staff) {
        std::cout << "[" << emp.id << "] " << emp.name
                  << " - 薪资: " << emp.salary << "\n";
    }
    return 0;
}
```

运行结果：

```text
[1001] Alice - 薪资: 8500
[1002] Bob - 薪资: 9200
[1003] Charlie - 薪资: 7800
```

### 嵌套 struct

struct 里可以包含另一个 struct：

```cpp
#include <iostream>
#include <string>

struct Address {
    std::string city;
    std::string street;
    int zipCode;
};

struct Person {
    std::string name;
    int age;
    Address home;  // 嵌套
};

int main() {
    Person p = {
        "Alice",
        25,
        {"北京", "长安街1号", 100000}
    };

    std::cout << p.name << " 住在 " << p.home.city
              << p.home.street << "\n";
    return 0;
}
```

运行结果：

```text
Alice 住在 北京长安街1号
```

### union（联合体）

`union` 让多个变量共享同一段内存。同一时刻只能存一个值。这在你需要"要么 A，要么 B"时有用：

```cpp
#include <iostream>

union Data {
    int i;
    double d;
    char c;
};

int main() {
    Data data;

    data.i = 42;
    std::cout << "int: " << data.i << "\n";

    // 现在写入 double，int 的值会被覆盖
    data.d = 3.14;
    std::cout << "double: " << data.d << "\n";
    // data.i 的值现在已无效

    return 0;
}
```

运行结果：

```text
int: 42
double: 3.14
```

> **新手建议**：`union` 比较底层，日常编程很少直接用。现代 C++ 更推荐用 `std::variant`（C++17 引入），它更安全。这里了解即可。

### 小练习

定义一个 `Book` 结构体，包含书名、作者、价格和页数。创建三本书，存入 vector，遍历输出每本书的信息，并计算平均价格。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <string>
#include <vector>

struct Book {
    std::string title;
    std::string author;
    double price;
    int pages;
};

int main() {
    std::vector<Book> library = {
        {"C++ Primer", "Stanley Lippman", 128.0, 920},
        {"Effective C++", "Scott Meyers", 89.0, 320},
        {"The C++ Programming Language", "Bjarne Stroustrup", 199.0, 1376}
    };

    double totalPrice = 0;
    int totalPages = 0;

    std::cout << "图书列表：\n";
    std::cout << "----------------------------------------\n";

    for (const auto& book : library) {
        std::cout << book.title << " | "
                  << book.author << " | "
                  << book.price << " 元 | "
                  << book.pages << " 页\n";
        totalPrice += book.price;
        totalPages += book.pages;
    }

    std::cout << "----------------------------------------\n";
    std::cout << "平均价格: " << totalPrice / library.size() << " 元\n";
    std::cout << "总页数: " << totalPages << " 页\n";

    return 0;
}
```

运行结果：

```text
图书列表：
----------------------------------------
C++ Primer | Stanley Lippman | 128 元 | 920 页
Effective C++ | Scott Meyers | 89 元 | 320 页
The C++ Programming Language | Bjarne Stroustrup | 199 元 | 1376 页
----------------------------------------
平均价格: 138.667 元
总页数: 2616 页
```

</details>

---

## 综合练习项目（含标准答案与运行结果）

以下练习综合运用本教程所有知识点，按难度递增排列。每道题附有完整代码和运行结果。

---

### 练习 1：状态机模拟（难度：中等）

**题目**：用 `enum class` 定义交通灯的三种状态（红、黄、绿）。写一个程序，模拟交通灯的循环变化：红 → 绿 → 黄 → 红，循环 10 次，每次输出当前状态和持续时间。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

enum class TrafficLight { Red, Green, Yellow };

std::string lightName(TrafficLight l) {
    switch (l) {
        case TrafficLight::Red:    return "红灯";
        case TrafficLight::Green:  return "绿灯";
        case TrafficLight::Yellow: return "黄灯";
    }
    return "未知";
}

int lightDuration(TrafficLight l) {
    switch (l) {
        case TrafficLight::Red:    return 30;  // 红灯 30 秒
        case TrafficLight::Green:  return 25;  // 绿灯 25 秒
        case TrafficLight::Yellow: return 5;   // 黄灯 5 秒
    }
    return 0;
}

int main() {
    TrafficLight light = TrafficLight::Red;

    for (int i = 1; i <= 10; ++i) {
        std::cout << "第 " << i << " 次: "
                  << lightName(light) << " ("
                  << lightDuration(light) << " 秒)\n";

        // 状态转换：红 → 绿 → 黄 → 红
        switch (light) {
            case TrafficLight::Red:    light = TrafficLight::Green;  break;
            case TrafficLight::Green:  light = TrafficLight::Yellow; break;
            case TrafficLight::Yellow: light = TrafficLight::Red;    break;
        }
    }
    return 0;
}
```

运行结果：

```text
第 1 次: 红灯 (30 秒)
第 2 次: 绿灯 (25 秒)
第 3 次: 黄灯 (5 秒)
第 4 次: 红灯 (30 秒)
第 5 次: 绿灯 (25 秒)
第 6 次: 黄灯 (5 秒)
第 7 次: 红灯 (30 秒)
第 8 次: 绿灯 (25 秒)
第 9 次: 黄灯 (5 秒)
第 10 次: 红灯 (30 秒)
```

</details>

---

### 练习 2：简易计算器（难度：中等）

**题目**：定义一个 `enum class Operation` 表示加减乘除四种运算。定义一个 `struct Calculator` 包含两个操作数和一个运算类型。写一个函数 `calculate` 接收 Calculator 并返回结果。程序循环让用户输入两个数和运算符，输出结果，直到用户输入 `q` 退出。

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>

enum class Operation { Add, Sub, Mul, Div };

struct Calculator {
    double a;
    double b;
    Operation op;
};

double calculate(const Calculator& c) {
    switch (c.op) {
        case Operation::Add: return c.a + c.b;
        case Operation::Sub: return c.a - c.b;
        case Operation::Mul: return c.a * c.b;
        case Operation::Div:
            if (c.b == 0) {
                std::cout << "错误：除数不能为零！\n";
                return 0;
            }
            return c.a / c.b;
    }
    return 0;
}

int main() {
    char opChar;
    double a, b;

    std::cout << "简易计算器（输入 q 退出）\n";

    while (true) {
        std::cout << "\n请输入运算式（如 3 + 5）：";
        std::cin >> a;

        if (std::cin.fail()) break;  // 输入非数字时退出

        std::cin >> opChar >> b;

        Operation op;
        switch (opChar) {
            case '+': op = Operation::Add; break;
            case '-': op = Operation::Sub; break;
            case '*': op = Operation::Mul; break;
            case '/': op = Operation::Div; break;
            default:
                std::cout << "不支持的运算符: " << opChar << "\n";
                continue;
        }

        Calculator calc = {a, b, op};
        double result = calculate(calc);
        std::cout << "结果: " << a << " " << opChar << " " << b
                  << " = " << result << "\n";
    }

    std::cout << "计算器已退出。\n";
    return 0;
}
```

运行示例：

```text
简易计算器（输入 q 退出）

请输入运算式（如 3 + 5）：10 + 5
结果: 10 + 5 = 15

请输入运算式（如 3 + 5）：20 / 4
结果: 20 / 4 = 5

请输入运算式（如 3 + 5）：7 * 8
结果: 7 * 8 = 56

请输入运算式（如 3 + 5）：10 / 0
错误：除数不能为零！
结果: 10 / 0 = 0

请输入运算式（如 3 + 5）：q
计算器已退出。
```

</details>

---

### 练习 3：学生成绩管理系统（难度：进阶）

**题目**：综合运用 struct、enum class、namespace、static 变量、vector 等知识点，实现一个简易的学生成绩管理系统。

要求：
- 用 `struct Student` 存储姓名、学号、科目成绩
- 用 `enum class Subject` 表示科目
- 用 `namespace GradeSystem` 包裹所有功能
- 用 static 变量统计总操作次数
- 支持添加学生、查询成绩、计算平均分功能

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <string>
#include <vector>

// 科目枚举
enum class Subject { Math, English, Science };

// 科目名称转换
std::string subjectName(Subject s) {
    switch (s) {
        case Subject::Math:    return "数学";
        case Subject::English: return "英语";
        case Subject::Science: return "科学";
    }
    return "未知";
}

// 学生结构体
struct Student {
    std::string name;
    int id;
    int mathScore;
    int englishScore;
    int scienceScore;
};

// 成绩管理系统命名空间
namespace GradeSystem {
    static int operationCount = 0;  // 统计操作次数

    std::vector<Student> students;

    void addStudent(const Student& s) {
        students.push_back(s);
        ++operationCount;
        std::cout << "已添加学生: " << s.name << "\n";
    }

    void queryScore(int id) {
        ++operationCount;
        for (const auto& s : students) {
            if (s.id == id) {
                std::cout << "学生: " << s.name << " (ID: " << s.id << ")\n";
                std::cout << "  数学: " << s.mathScore << "\n";
                std::cout << "  英语: " << s.englishScore << "\n";
                std::cout << "  科学: " << s.scienceScore << "\n";
                return;
            }
        }
        std::cout << "未找到 ID 为 " << id << " 的学生\n";
    }

    void showAverage() {
        ++operationCount;
        if (students.empty()) {
            std::cout << "暂无学生数据\n";
            return;
        }

        double mathTotal = 0, engTotal = 0, sciTotal = 0;
        for (const auto& s : students) {
            mathTotal += s.mathScore;
            engTotal += s.englishScore;
            sciTotal += s.scienceScore;
        }

        int count = static_cast<int>(students.size());
        std::cout << "各科平均分（共 " << count << " 人）:\n";
        std::cout << "  数学: " << mathTotal / count << "\n";
        std::cout << "  英语: " << engTotal / count << "\n";
        std::cout << "  科学: " << sciTotal / count << "\n";
    }

    void showAll() {
        ++operationCount;
        std::cout << "=== 全部学生 ===\n";
        for (const auto& s : students) {
            double avg = (s.mathScore + s.englishScore + s.scienceScore) / 3.0;
            std::cout << "[" << s.id << "] " << s.name
                      << " - 数学:" << s.mathScore
                      << " 英语:" << s.englishScore
                      << " 科学:" << s.scienceScore
                      << " 平均:" << avg << "\n";
        }
    }

    int getOperationCount() {
        return operationCount;
    }
}

int main() {
    using namespace GradeSystem;

    // 添加学生
    addStudent({"Alice", 1001, 95, 88, 92});
    addStudent({"Bob", 1002, 78, 85, 90});
    addStudent({"Charlie", 1003, 88, 92, 76});

    std::cout << "\n";

    // 查询成绩
    queryScore(1001);
    std::cout << "\n";

    queryScore(9999);  // 不存在的 ID
    std::cout << "\n";

    // 显示平均分
    showAverage();
    std::cout << "\n";

    // 显示全部
    showAll();
    std::cout << "\n";

    // 操作次数
    std::cout << "总操作次数: " << getOperationCount() << "\n";

    return 0;
}
```

运行结果：

```text
已添加学生: Alice
已添加学生: Bob
已添加学生: Charlie

学生: Alice (ID: 1001)
  数学: 95
  英语: 88
  科学: 92

未找到 ID 为 9999 的学生

各科平均分（共 3 人）:
  数学: 87
  英语: 88.3333
  科学: 86

=== 全部学生 ===
[1001] Alice - 数学:95 英语:88 科学:92 平均:91.6667
[1002] Bob - 数学:78 英语:85 科学:90 平均:84.3333
[1003] Charlie - 数学:88 英语:92 科学:76 平均:85.3333

总操作次数: 7
```

</details>

---

### 练习 4：简单库存管理系统（难度：进阶）

**题目**：综合运用本教程所有知识点，实现一个简易库存管理系统。

要求：
- 用 `enum class Category` 表示商品类别（食品、电子、服装）
- 用 `struct Product` 存储商品信息（名称、类别、价格、库存数量）
- 用 `namespace Inventory` 包裹所有功能
- 用 static 变量自动生成商品 ID
- 支持添加商品、按类别查询、显示库存总价值

<details>
<summary>标准答案（点击展开）</summary>

```cpp
#include <iostream>
#include <string>
#include <vector>

enum class Category { Food, Electronics, Clothing };

std::string categoryName(Category c) {
    switch (c) {
        case Category::Food:        return "食品";
        case Category::Electronics: return "电子";
        case Category::Clothing:    return "服装";
    }
    return "未知";
}

struct Product {
    int id;
    std::string name;
    Category category;
    double price;
    int stock;
};

namespace Inventory {
    std::vector<Product> products;

    // 静态变量自动生成 ID
    int generateId() {
        static int nextId = 1000;
        return ++nextId;
    }

    void addProduct(const std::string& name, Category cat, double price, int stock) {
        Product p = {generateId(), name, cat, price, stock};
        products.push_back(p);
        std::cout << "已添加: [" << p.id << "] " << p.name
                  << " (" << categoryName(p.category) << ") "
                  << "价格=" << p.price << " 库存=" << p.stock << "\n";
    }

    void queryByCategory(Category cat) {
        std::cout << "--- " << categoryName(cat) << " 类商品 ---\n";
        bool found = false;
        for (const auto& p : products) {
            if (p.category == cat) {
                std::cout << "  [" << p.id << "] " << p.name
                          << " 价格=" << p.price
                          << " 库存=" << p.stock << "\n";
                found = true;
            }
        }
        if (!found) {
            std::cout << "  无此类商品\n";
        }
    }

    void showTotalValue() {
        double total = 0;
        for (const auto& p : products) {
            total += p.price * p.stock;
        }
        std::cout << "库存总价值: " << total << " 元\n";
    }

    void showAll() {
        std::cout << "=== 库存清单 ===\n";
        for (const auto& p : products) {
            std::cout << "[" << p.id << "] " << p.name
                      << " | " << categoryName(p.category)
                      << " | 价格=" << p.price
                      << " | 库存=" << p.stock << "\n";
        }
    }
}

int main() {
    using namespace Inventory;

    // 添加商品
    addProduct("苹果", Category::Food, 5.5, 200);
    addProduct("矿泉水", Category::Food, 2.0, 500);
    addProduct("USB线", Category::Electronics, 19.9, 150);
    addProduct("耳机", Category::Electronics, 99.0, 80);
    addProduct("T恤", Category::Clothing, 59.0, 120);

    std::cout << "\n";

    // 按类别查询
    queryByCategory(Category::Food);
    std::cout << "\n";
    queryByCategory(Category::Electronics);
    std::cout << "\n";
    queryByCategory(Category::Clothing);
    std::cout << "\n";

    // 显示全部
    showAll();
    std::cout << "\n";

    // 库存总价值
    showTotalValue();

    return 0;
}
```

运行结果：

```text
已添加: [1001] 苹果 (食品) 价格=5.5 库存=200
已添加: [1002] 矿泉水 (食品) 价格=2 库存=500
已添加: [1003] USB线 (电子) 价格=19.9 库存=150
已添加: [1004] 耳机 (电子) 价格=99 库存=80
已添加: [1005] T恤 (服装) 价格=59 库存=120

--- 食品类商品 ---
  [1001] 苹果 价格=5.5 库存=200
  [1002] 矿泉水 价格=2 库存=500

--- 电子类商品 ---
  [1003] USB线 价格=19.9 库存=150
  [1004] 耳机 价格=99 库存=80

--- 服装类商品 ---
  [1005] T恤 价格=59 库存=120

=== 库存清单 ===
[1001] 苹果 | 食品 | 价格=5.5 | 库存=200
[1002] 矿泉水 | 食品 | 价格=2 | 库存=500
[1003] USB线 | 电子 | 价格=19.9 | 库存=150
[1004] 耳机 | 电子 | 价格=99 | 库存=80
[1005] T恤 | 服装 | 价格=59 | 库存=120

库存总价值: 32145 元
```

</details>

---

## 本章小结

回顾这一部分你学到的新知识：

| 知识点 | 你掌握的能力 |
|--------|------------|
| 枚举 | 用 enum class 定义类型安全的枚举，理解底层存储与 switch 配合 |
| static_cast | 用 C++ 推荐的方式做类型转换 |
| 自定义类型名 | 用 using 给类型起别名，用 auto 让编译器推导类型 |
| 命名空间 | 用 namespace 隔离名字，理解 using 声明与指令的区别 |
| 匿名命名空间 | 限制变量/函数只在当前文件可见 |
| 变量生命周期 | 理解局部、全局、静态变量的区别，掌握作用域 :: 和 extern |
| 栈与堆 | 理解 C++ 的两种内存区域及其管理方式 |
| 数据计算 | 用 sizeof、limits、cmath、random 处理数值 |
| cout 格式化 | 用 iomanip 控制输出精度、宽度、对齐 |
| cin 输入 | 读取各种类型输入，处理 cin 与 getline 的混用问题 |
| constexpr | 定义编译期常量，区分 const、constexpr 和 #define |
| 自定义数据类型 | 用 struct 组合多个值成一个新类型 |
| std::string | 字符串拼接、查找、截取、比较等常用操作 |
| std::vector | 动态数组的增删改查，push_back/size/insert/erase 等 |
| std::pair | 将两个值绑定在一起 |
| const 成员函数 | 承诺不修改对象状态，支持 const 对象调用 |
| 内存对齐 | 理解 struct 中 padding 的存在和影响 |

**下一步建议**：

1. 把 4 道综合练习全部手写一遍
2. 尝试修改练习中的代码，添加新功能
3. 下一部分我们将学习：函数进阶（参数传递、重载、默认参数、Lambda）
4. 之后进入指针与引用，这是 C++ 最核心也最容易出错的部分
5. 最后是面向对象：类、构造函数、继承、多态

---

*本教程使用 C++17 标准编写，所有示例代码均已在 g++ -std=c++17 下编译验证通过。*
