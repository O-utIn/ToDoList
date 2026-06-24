# 极简待办清单 (Minimalist ToDo List)

<p align="center">
  <strong>习惯打卡 · 待办清单 · 番茄专注 · AI 智能助手</strong>
</p>

<p align="center">
  一款面向学生与职场人士的极简风 Android 时间管理工具，融合四大效率模块与 DeepSeek AI 自然语言操作。
</p>

---

## 项目简介

**极简待办清单** 是一款 Android 时间管理应用，以"极简主义"为设计核心。除传统的待办与习惯管理外，内置番茄钟计时器与 DeepSeek AI 对话助手，支持通过自然语言直接创建、修改、删除、查询待办事项、习惯和番茄钟任务。

本项目为《移动互联网应用》课程设计作品，涵盖了 Android 开发中的多项核心技术。

### 五大板块

| 板块 | 导航 | 功能描述 |
|:---|:---|:---|
| **今日习惯** | 🌱 | 每日习惯打卡，折叠月历/周历，滑动删除与撤销，支持每日/每周指定天/特定日期模式 |
| **今日待办** | ✅ | 优先级色带、截止日期智能显示、勾选完成、滑动删除+撤销，按日期过滤 |
| **番茄钟** | ⏱️ | 前台服务保活、三段式循环（工作→短休→长休）、通知栏同步、时长芯片高亮选择、任务管理 |
| **AI 助手** | 🤖 | DeepSeek 对话、自然语言 CRUD（12 种命令）、命令协议自动执行、本地推荐引擎 |
| **我的** | 👤 | 登录/注册/记住密码、统计面板、后台自动定位、备份恢复、密码锁、AI 配置 |

---

## 技术架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                       表示层 (UI Layer)                              │
│  SplashActivity → LockActivity → MainActivity                      │
│  HabitFragment | TodoFragment | PomodoroFragment | ChatFragment     │
│  MineFragment | EditTodoActivity | EditHabitActivity                │
│  PomodoroTimerActivity | PomodoroSettingsActivity                   │
└─────────────────────────────────────────────────────────────────────┘
│
┌─────────────────────────────────────────────────────────────────────┐
│                     业务逻辑层 (Logic Layer)                         │
│  PomodoroService (Foreground)  │ CommandParser (12 种 CMD 操作)     │
│  ForceOfflineReceiver          │ RecommendationEngine              │
│  DailyReminderReceiver         │ NotificationHelper (4 通道)        │
│  NotificationActionReceiver    │ LocationHelper (LiveData)          │
└─────────────────────────────────────────────────────────────────────┘
│
┌─────────────────────────────────────────────────────────────────────┐
│                     数据源与支撑层 (Data Layer)                       │
│  Room/SQLite (7 entities)      │ SharedPreferences (config/session) │
│  ContentProvider (TodoProvider) │ File Storage (JSON backup)        │
│  Retrofit + OkHttp (DeepSeek)  │ Android Keystore (AES-256/GCM)    │
│  FusedLocationProviderClient   │ AlarmManager                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 技术栈

| 类别 | 技术 |
|:---|:---|
| **语言** | Java, Kotlin |
| **UI** | Jetpack Compose + Material 3, ViewBinding, ConstraintLayout, RecyclerView |
| **架构** | Room, ViewModel, LiveData, Fragment, ViewPager2 |
| **后台** | Foreground Service (`specialUse`), AlarmManager, BroadcastReceiver, CountDownTimer |
| **网络** | Retrofit 2 + OkHttp + Gson (DeepSeek compatible API) |
| **安全** | Android Keystore AES-256/GCM, SHA-256 |
| **位置** | Google Play Services FusedLocationProviderClient, Geocoder |
| **构建** | Gradle (Kotlin DSL) + Version Catalog (`libs.versions.toml`) |

---

## 项目结构

```
ToDoList/
├── app/
│   ├── src/main/java/com/example/todolist/
│   │   ├── ai/                         # AI 模块
│   │   │   ├── ChatClient.java         #   DeepSeek Chat API 客户端 (单例)
│   │   │   ├── ChatApi.java            #   Retrofit 接口
│   │   │   ├── ChatRequest.java        #   请求体
│   │   │   ├── ChatResponse.java       #   响应体
│   │   │   ├── CommandParser.java      #   [CMD] 协议解析与执行器 (12 种操作)
│   │   │   └── recommendation/
│   │   │       └── RecommendationEngine.java  # 规则推荐引擎
│   │   ├── adapter/                    # RecyclerView 适配器
│   │   │   ├── HabitAdapter.java
│   │   │   ├── TodoAdapter.java
│   │   │   ├── PomodoroAdapter.java
│   │   │   ├── ChatAdapter.java        #   双视图 (发送/接收)
│   │   │   └── CalendarMonthAdapter.java  # 42 格月历 + 角标
│   │   ├── data/
│   │   │   ├── AppDatabase.java        # Room 数据库 (v9, 双检锁单例)
│   │   │   ├── dao/                    # 6 个 DAO 接口
│   │   │   │   ├── TodoDao.java
│   │   │   │   ├── HabitDao.java
│   │   │   │   ├── HabitCheckDao.java
│   │   │   │   ├── PomodoroTaskDao.java
│   │   │   │   ├── PomodoroSessionDao.java
│   │   │   │   └── ChatMessageDao.java
│   │   │   └── entity/                 # 7 个实体类
│   │   │       ├── TodoItem.java
│   │   │       ├── HabitItem.java      #   含 schedule_config JSON 解析
│   │   │       ├── HabitCheck.java
│   │   │       ├── HabitCheckin.java
│   │   │       ├── PomodoroTask.java
│   │   │       ├── PomodoroSession.java
│   │   │       └── ChatMessage.java
│   │   ├── fragments/                  # 6 个 Fragment
│   │   │   ├── HabitFragment.java      #   习惯打卡 + 日历
│   │   │   ├── TodoFragment.java       #   待办列表 + 日历
│   │   │   ├── PomodoroFragment.java   #   番茄钟状态 + 任务列表
│   │   │   ├── ChatFragment.java       #   AI 对话 (CMD 协议 + 重试)
│   │   │   ├── DiscoverFragment.java   #   ChatFragment 容器
│   │   │   └── MineFragment.java       #   个人中心 (统计+定位+备份)
│   │   ├── provider/
│   │   │   └── TodoProvider.java       # ContentProvider (跨进程数据共享)
│   │   ├── receiver/
│   │   │   ├── ForceOfflineReceiver.java       # 强制下线广播
│   │   │   ├── DailyReminderReceiver.java      # 每日提醒广播
│   │   │   └── NotificationActionReceiver.java # 番茄钟通知操作
│   │   ├── service/
│   │   │   └── PomodoroService.java    # 番茄钟前台服务 (三段式循环)
│   │   ├── util/
│   │   │   ├── BackupManager.java      # JSON 导出/导入 (双存储)
│   │   │   ├── PreferencesManager.java # SharedPreferences 统一接口
│   │   │   ├── NotificationHelper.java # 通知管理 (4 通道)
│   │   │   ├── LocationHelper.java     # GPS 定位 + 反向地理编码 (单例 + LiveData)
│   │   │   ├── CryptoUtils.java        # AES-256/GCM 加解密 (Android Keystore)
│   │   │   ├── HashUtils.java          # SHA-256 哈希
│   │   │   ├── DateUtils.java          # 日期工具 (java.time)
│   │   │   └── UserSession.java        # 当前用户读取
│   │   ├── MainActivity.java           # 主 Activity (底部导航 + 自动定位)
│   │   ├── LoginActivity.java          # 登录/注册
│   │   ├── LockActivity.java           # 密码锁
│   │   ├── SplashActivity.java         # 启动页 (励志语录)
│   │   ├── EditTodoActivity.java       # 编辑待办
│   │   ├── EditHabitActivity.java      # 编辑习惯
│   │   ├── PomodoroTimerActivity.java  # 全屏番茄钟
│   │   └── PomodoroSettingsActivity.java # 番茄钟设置
│   ├── src/main/res/
│   │   ├── layout/        # 布局文件
│   │   ├── drawable/      # 矢量图标与背景
│   │   ├── menu/          # 底部导航菜单
│   │   ├── mipmap/        # 自适应启动图标
│   │   ├── values/        # 颜色/字符串/主题
│   │   └── xml/           # 备份规则
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── Prd.md
└── README.md
```

---

## 快速开始

### 环境要求

- **Android Studio** 2025.1 或更高版本
- **Android SDK** 36 (compileSdk) / 最低支持 API 24 (Android 7.0)
- **JDK** 11 或更高
- **Gradle** 8.x (使用 Wrapper，无需手动安装)

### 构建与运行

```bash
# 1. 克隆仓库
git clone https://github.com/O-utIn/ToDoList.git
cd ToDoList

# 2. 配置 Android SDK 路径
# 编辑 local.properties，添加：
# sdk.dir=/path/to/your/Android/sdk

# 3. 构建 Debug APK
./gradlew assembleDebug         # Linux / macOS
gradlew.bat assembleDebug       # Windows

# 4. 安装到已连接设备
./gradlew installDebug
```

### AI 助手配置

1. 启动 App，切换到底部"AI助手"标签
2. 点击弹出的 API Key 对话框
3. 输入 [DeepSeek API Key](https://platform.deepseek.com/api_keys)
4. 保存后即可使用自然语言对话操作待办、习惯和番茄钟

---

## 特色功能详解

### AI 自然语言操作 (CMD Protocol)

App 内置一套命令协议，AI 在回复中嵌入 `[CMD]{"action":"...","参数":"值"}[/CMD]` JSON 块，由本地 `CommandParser` 解析后直接操作数据库。支持 12 种操作：

| 命令 | 用途 |
|:---|:---|
| `create_todo` / `delete_todo` / `complete_todo` / `update_todo` | 待办 CRUD |
| `create_habit` / `delete_habit` / `update_habit` | 习惯 CRUD |
| `create_pomo` / `delete_pomo` / `update_pomo` | 番茄钟 CRUD |
| `list_todos` / `list_habits` / `list_pomos` | 列表查询 |

> 示例：_用户："把买菜改成买菜和水果"_ → AI 自动发出 `update_todo` 命令修改标题。

### 本地智能推荐引擎

基于规则评分（时间槽匹配 ×2.0 + 历史完成率 ×3.0 + 今日未打卡奖励 ×1.0），在设备本地实时推荐最适合当前时段的习惯，无需联网。

### 番茄钟前台服务

`PomodoroService` (Foreground Service) 驱动倒计时，三段式自动循环（工作→短休息→长休息）。进程死亡后自动恢复状态，通知栏实时显示进度与操作按钮。设置页时长芯片选中高亮（黄色填充）。

### 后台自动定位

App 启动时自动在后台获取当前位置。切换到"我的"页面时，位置卡片已显示最新的地址、坐标和精度信息，无需手动刷新。

### 多层安全

- **登录密码**：SHA-256 哈希存储
- **记住密码**：Android Keystore 硬件级 AES-256/GCM 加密，密钥不离开设备
- **应用锁**：可选密码保护，冷启动强制验证
- **强制下线**：多端登录冲突检测，`ForceOfflineReceiver` 弹出不可取消对话框

### 数据备份与恢复

`BackupManager` 导出所有待办和习惯数据为 JSON 文件，双存储策略（应用私有目录 + 公共 Downloads 文件夹），支持从文件或 URI 恢复（追加式导入，不覆盖已有数据）。

### ContentProvider 跨进程共享

`TodoProvider` (authority: `com.example.todolist.provider`) 暴露待办列表和计数，供其他应用查询。

---

## 数据库设计

| 表名 | 实体类 | 用途 | 关键字段 |
|:---|:---|:---|:---|
| `todo_item` | `TodoItem` | 待办事项 | title, priority(1-3), due_date, is_completed |
| `habit_item` | `HabitItem` | 习惯元数据 | name, schedule_config (JSON), frequency, color |
| `habit_check` | `HabitCheck` | 每日打卡 | habit_id, date_stamp (yyyyMMdd), checked |
| `habit_checkin` | `HabitCheckin` | 打卡 (新版) | habitId, dateStamp, isCompleted |
| `pomodoro_task` | `PomodoroTask` | 番茄任务 | name, duration_minutes |
| `pomodoro_session` | `PomodoroSession` | 番茄会话 | task_id (FK), start_time, end_time, completed |
| `chat_message` | `ChatMessage` | AI 聊天 | content, type(0=AI/1=user), session_id |

所有数据表均带 `user_id` 字段实现多用户数据隔离。数据库使用 `fallbackToDestructiveMigration` 策略，当前版本 v9。

---

## 界面预览

应用采用 Material Design 3 风格，主色为温暖黄色系 (`#FFD54F` / `#FFC107`)，5 个底部导航标签：

| 导航 | 核心交互 |
|:---|:---|
| 习惯 | 折叠月历 / 打卡 / 左滑删除 + 撤销 |
| 待办 | 优先级色带 / 智能日期 / 勾选完成 |
| 番茄钟 | 圆形倒计时 / 三段循环 / 通知操作 / 时长芯片高亮 |
| AI助手 | 气泡对话 / 自然语言 CRUD（12 种命令） |
| 我的 | emoji 头像 / 统计面板 / 后台自动定位 / 备份恢复 |

---

## 许可证

本项目为课程设计作品，仅供学习与参考。

---

## 致谢

- [Material Design](https://material.io/design) — Google 设计语言
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — 现代化 Android UI 工具包
- [Android Room](https://developer.android.com/training/data-storage/room) — 本地持久化库
- [Retrofit](https://square.github.io/retrofit/) — HTTP 客户端
- [DeepSeek](https://platform.deepseek.com/) — AI 大语言模型 API
