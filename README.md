# 极简待办清单 (Minimalist ToDo List)

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="App Icon" width="120" />
</p>

<p align="center">
  一款融合了<strong>习惯打卡</strong>、<strong>待办清单</strong>与<strong>番茄专注</strong>的极简风时间管理 Android 应用。
</p>

---

## 📖 项目简介

**极简待办清单** 是一款面向学生群体与职场办公人员的 Android 时间管理工具。应用以"极简主义"为设计核心，帮助用户克服拖延症、培养良好习惯，并提供智能化的任务推荐。

本项目作为《移动互联网应用》课程设计作品，涵盖了 Android 开发中的多项核心技术。

### 核心功能板块

| 板块 | 图标 | 功能描述 |
|:---|:---|:---|
| **今日习惯** | 🌱 | 每日习惯打卡，支持日期切换、快速打卡、习惯创建与管理，含智能推荐 |
| **今日待办** | ✅ | 待办事项管理，支持优先级设置、滑动删除/编辑、截止日期与备注 |
| **番茄钟** | ⏱️ | 番茄工作法计时器，前台服务保活、圆形进度条、白噪音播放、通知栏同步 |
| **我的** | 👤 | 个人中心，数据统计面板、密码锁、数据备份恢复、每日提醒、位置服务 |

---

## 🏗️ 技术架构

```
+-------------------------------------------------------------------+
|                           表示层 (UI Layer)                        |
|   Activity (Login, Main, Edit) | Fragment (Habit, Todo, Pomodoro, |
|   Discover, Mine) | RecyclerView | ViewPager2 | Custom Timer View  |
+-------------------------------------------------------------------+
|                          业务逻辑层 (Logic Layer)                  |
|   ViewModel | BroadcastReceiver (强制下线, 每日提醒)                |
|   Foreground Service (番茄钟倒计时) | AI Command Parser             |
+-------------------------------------------------------------------+
|                          数据源与支撑层 (Data Layer)                |
|   Room/SQLite | SharedPreferences | File Storage                   |
|   ContentProvider | Retrofit (AI API) | Location API               |
+-------------------------------------------------------------------+
```

### 技术栈

| 类别 | 技术 |
|:---|:---|
| **语言** | Java, Kotlin |
| **UI 框架** | Jetpack Compose + Material 3, ViewBinding, ConstraintLayout |
| **架构组件** | ViewModel, LiveData, Room, ViewPager2, Fragment |
| **后台任务** | Foreground Service, AlarmManager, BroadcastReceiver |
| **数据持久化** | Room (SQLite), SharedPreferences, File Storage |
| **网络** | Retrofit 2 + OkHttp + Gson |
| **位置服务** | Google Play Services Location API |
| **多媒体** | MediaPlayer (白噪音播放) |
| **构建工具** | Gradle (Kotlin DSL) + Version Catalog |

---

## 📂 项目结构

```
ToDoList/
├── app/
│   ├── src/main/java/com/example/todolist/
│   │   ├── ai/                    # AI 智能解析与推荐模块
│   │   │   ├── AiClient.java      # AI API 客户端
│   │   │   ├── AiApi.java         # API 接口定义
│   │   │   ├── CommandParser.java # 自然语言命令解析
│   │   │   ├── ChatClient.java    # 聊天 AI 客户端
│   │   │   └── recommendation/    # 本地推荐引擎
│   │   ├── adapter/               # RecyclerView 适配器
│   │   │   ├── HabitAdapter.java
│   │   │   ├── TodoAdapter.java
│   │   │   ├── PomodoroAdapter.java
│   │   │   ├── CalendarMonthAdapter.java
│   │   │   └── ChatAdapter.java
│   │   ├── data/
│   │   │   ├── AppDatabase.java   # Room 数据库
│   │   │   ├── dao/               # 数据访问对象
│   │   │   └── entity/            # 数据实体类
│   │   ├── fragments/             # Fragment 模块
│   │   │   ├── HabitFragment.java # 今日习惯
│   │   │   ├── TodoFragment.java  # 今日待办
│   │   │   ├── PomodoroFragment.java # 番茄钟
│   │   │   ├── DiscoverFragment.java # 发现页
│   │   │   ├── MineFragment.java  # 个人中心
│   │   │   ├── ChatFragment.java  # 反馈聊天
│   │   │   └── LocationFragment.java # 位置推荐
│   │   ├── provider/
│   │   │   └── TodoProvider.java  # ContentProvider 跨进程共享
│   │   ├── receiver/              # 广播接收器
│   │   │   ├── ForceOfflineReceiver.java  # 强制下线
│   │   │   ├── DailyReminderReceiver.java # 每日提醒
│   │   │   └── NotificationActionReceiver.java
│   │   ├── service/
│   │   │   └── PomodoroService.java # 番茄钟前台服务
│   │   ├── util/                  # 工具类
│   │   │   ├── BackupManager.java # 数据备份恢复
│   │   │   ├── PreferencesManager.java
│   │   │   ├── NotificationHelper.java
│   │   │   ├── LocationHelper.java
│   │   │   ├── CryptoUtils.java  # 加密工具
│   │   │   └── HashUtils.java    # 哈希工具
│   │   ├── MainActivity.java     # 主 Activity
│   │   ├── LoginActivity.java    # 登录
│   │   ├── LockActivity.java     # 密码锁
│   │   ├── SplashActivity.java   # 启动页
│   │   ├── EditTodoActivity.java # 编辑待办
│   │   ├── EditHabitActivity.java # 编辑习惯
│   │   ├── PomodoroTimerActivity.java  # 番茄钟计时
│   │   └── PomodoroSettingsActivity.java # 番茄钟设置
│   ├── src/main/res/
│   │   ├── layout/               # XML 布局文件
│   │   ├── drawable/             # 矢量图标与背景
│   │   ├── mipmap/               # 应用图标
│   │   ├── values/               # 颜色、字符串等资源
│   │   └── xml/                  # 备份规则等配置
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml        # Version Catalog 依赖管理
├── build.gradle.kts              # 顶级构建脚本
├── settings.gradle.kts
├── gradle.properties
├── Prd.md                        # 产品需求文档
└── README.md
```

---

## 🚀 快速开始

### 环境要求

- **Android Studio** Meerkat (2025.1.1) 或更高版本
- **Android SDK** 36 (compileSdk) / 最低支持 API 24 (Android 7.0)
- **JDK** 11 或更高
- **Gradle** 8.x (使用 Wrapper，无需手动安装)

### 构建与运行

```bash
# 1. 克隆仓库
git clone https://github.com/O-utIn/ToDoList.git
cd ToDoList

# 2. 配置本地属性（Android SDK 路径）
# 编辑 local.properties，设置你的 SDK 路径：
# sdk.dir=/path/to/your/Android/sdk

# 3. 构建 Debug APK
./gradlew assembleDebug       # Linux/macOS
gradlew.bat assembleDebug     # Windows

# 4. 安装到设备
./gradlew installDebug

# 5. 运行测试
./gradlew test
./gradlew connectedAndroidTest
```

---

## 🔑 特色功能详解

### 🤖 本地 AI 智能推荐
基于规则的轻量级决策引擎，根据当前时间、地理位置和历史打卡频次，在"今日习惯"顶部智能推荐最适合当前时段的习惯。

### 🔒 密码锁保护
使用 SHA-256 哈希存储密码，支持"记住密码"选项。每次冷启动或从后台恢复时自动校验。

### 💾 数据备份与恢复
支持将 SQLite 中的待办与习惯数据导出为本地 `.json` 备份文件，并支持从备份文件恢复。

### 📡 强制下线机制
通过自定义 BroadcastReceiver 实现多端登录冲突检测，当接收到强制下线广播时，弹出不可取消对话框并跳转至登录页面。

### 🔗 ContentProvider 跨进程共享
自定义 `TodoProvider`，暴露待办事项的部分访问权限，供其他应用或桌面小部件读取。

### 📍 基于位置的习惯推荐
结合 Location API 获取用户地理位置标签，根据场景智能推荐习惯（如运动场推荐"跑步"）。

### ⏲️ 番茄钟前台服务
使用 Foreground Service 确保倒计时在应用进入后台或锁屏时不中断，通知栏实时同步进度并支持快捷操作。

### 💬 AI 自然语言输入
支持通过自然语言快速创建待办事项，AI 自动解析时间、优先级等信息。

---

## 📊 数据持久化设计

| 数据表 | 存储方式 | 用途 |
|:---|:---|:---|
| `todo_item` | Room (SQLite) | 待办事项元数据 |
| `habit_item` | Room (SQLite) | 习惯元数据 |
| `habit_check` | Room (SQLite) | 每日打卡记录 |
| `pomodoro_task` | Room (SQLite) | 番茄钟任务 |
| `pomodoro_session` | Room (SQLite) | 番茄钟会话记录 |
| `chat_message` | Room (SQLite) | 反馈聊天消息 |
| 打卡状态戳 | SharedPreferences | 快速打卡状态 |
| 用户配置 | SharedPreferences | 密码、偏好设置 |
| 备份文件 | File Storage | JSON 格式数据导出 |

---

## 📝 许可证

本项目为课程设计作品，仅供学习与参考。

---

## 🙏 致谢

- [Material Design](https://material.io/design) - Google 设计语言
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代化 Android UI 工具包
- [Android Room](https://developer.android.com/training/data-storage/room) - 本地持久化库
- [Retrofit](https://square.github.io/retrofit/) - HTTP 客户端
