# 学之舟 - 安卓版（测试）
**这是学之舟的安卓实现的测试版本**

# 程序目录结构

```
boatofstudy/
├── app/                        # 主应用模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/boatofstudy/knowledge/
│   │   │   │   └── MainActivity.java    # 主活动类
│   │   │   └── res/
│   │   │       └── layout/
│   │   │           └── main.xml         # 主界面布局
│   │   └── AndroidManifest.xml          # 应用配置文件
│   └── build.gradle                     # 模块构建配置
├── build.gradle                         # 项目构建配置
└── settings.gradle                      # 项目设置文件
```

文件类型说明：
- `.java` : Java源代码文件
- `.xml`  : 布局文件或配置文件
  - `AndroidManifest.xml` : 每个应用必须且唯一的配置文件
- `.gradle` : 项目构建配置文件
