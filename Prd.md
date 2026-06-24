# "极简待办清单 APP" 产品需求文档（PRD）

---

## 1. 文档概述

### 1.1 编写目的
本产品需求文档（PRD）旨在详细定义"极简待办清单 APP"的功能需求、界面逻辑、技术实现路径以及非功能性要求。本文档作为该移动互联网应用课程设计项目的指导性技术文件，用于规范后续的开发、测试与项目报告撰写。

### 1.2 项目背景
在快节奏的现代生活中，时间管理与习惯养成逐渐成为用户的刚性需求。本项目参考了主流"极简待办清单"类应用，旨在设计并实现一款界面清新、交互简单且功能实用的待办与习惯管理工具。该应用融合了番茄工作法、AI 智能对话助手、本地数据存储、广播通信、后台服务、位置服务及本地推荐算法，用以检验和展示在《移动互联网应用》课程中所掌握的 Android 开发技术。

---

## 2. 产品定位与目标用户

*   **产品定位**：一款融合了"习惯打卡"、"待办清单（ToDo）"、"番茄专注（Pomodoro）"与"AI 智能助手"的极简风时间管理工具。
*   **目标用户**：学生群体、职场办公人员，以及需要克服拖延症、培养良好生活与工作习惯的效率追求者。
*   **设计原则**：
    *   **极简主义**：界面视觉留白得当，操作路径尽可能缩短。
    *   **高内聚低耦合**：功能模块边界清晰，代码模块化。
    *   **多维数据持久化**：针对不同数据类型，合理采用多样化的存储策略。

---

## 3. 系统技术架构设计

为确保系统功能的合理性与扩展性，技术架构采用经典的分层设计：

```
+-------------------------------------------------------------------+
|                           表示层 (UI Layer)                        |
|   Activity (Splash, Login, Lock, Main, EditTodo, EditHabit,       |
|             PomodoroTimer, PomodoroSettings)                       |
|   Fragment (Habit, Todo, Pomodoro, Discover/Chat, Mine)            |
|   RecyclerView, ViewPager2, BottomNavigationView, Custom Views     |
+-------------------------------------------------------------------+
|                          业务逻辑层 (Logic Layer)                  |
|   BroadcastReceiver (强制下线, 每日提醒, 通知操作)                    |
|   Foreground Service (番茄钟倒计时)   |   AI Command Parser         |
|   RecommendationEngine (本地规则推荐) |   通知管理 (Notification)    |
+-------------------------------------------------------------------+
|                          数据源与支撑层 (Data Layer)                |
|   Room/SQLite (7 张实体表)     |   SharedPreferences (配置与会话)    |
|   File Storage (备份/日志)     |   ContentProvider (跨进程共享)      |
|   Location API (FusedLocation) |   Retrofit + OkHttp (AI API)      |
|   Android Keystore (AES/GCM)   |   AlarmManager (定时提醒)         |
+-------------------------------------------------------------------+
```

---

## 4. 功能模块详细需求

应用分为五大板块（通过底部导航栏切换）：**今日习惯**、**今日待办**、**番茄钟**、**AI 助手**、**我的（个人中心）**。

### 4.1 核心板块一：今日习惯 (Today's Habits)

#### 4.1.1 界面交互说明
*   **顶部日历栏**：支持折叠/展开双模式（周视图 / 月视图），左右箭头切换月份或周。默认选中当天。
*   **日期标记**：日历格子右上角显示当天已完成打卡数量的角标。
*   **列表展示**：以卡片形式按 `schedule_config` 过滤展示当日应出现的习惯。习惯卡片包含彩色圆点、习惯名称、打卡状态（已完成/未完成）、频率摘要。
*   **快速打卡**：点击习惯卡片上的勾选框，伴随轻微动画切换打卡状态，日历角标同步刷新。
*   **滑动删除**：支持左滑删除习惯（弹出确认对话框），支持撤销删除（Snackbar）。
*   **添加/编辑**：点击 FAB 或习惯卡片跳转 `EditHabitActivity`，支持创建与编辑已有习惯。

#### 4.1.2 关键技术点实现
*   **UI 布局**：`CalendarMonthAdapter`（42 格月历 + 7 列星期头部），`HabitAdapter` 结合 `ItemTouchHelper` 实现滑动删除。
*   **Fragment 设计**：`HabitFragment` 承载本模块，通过 `MainActivity.getSelectedDate()` 与其他 Fragment 共享日期状态。
*   **数据持久化**：
    *   `habit_item` 表（Room）存储习惯元数据（名称、描述、图标、频率、颜色、`schedule_config` JSON）。
    *   `habit_check` 表存储逐日打卡记录（`habit_id` + `date_stamp` + `checked`）。
    *   `schedule_config` 支持三种模式：`daily`（每日）、`weekly`（每周指定天，如 `"days":"124"` 表示周一/二/四）、`specific`（指定单日）。

### 4.2 核心板块二：今日待办 (Today's ToDos)

#### 4.2.1 界面交互说明
*   **顶部日历栏**：与习惯模块共用同一日历组件，支持折叠展开与角标显示。
*   **待办列表**：按日期过滤显示（`due_date == 0` 或 `due_date` 属于选中日期），按未完成优先、截止日期、优先级降序排列。
*   **列表项信息**：左侧优先级色带（红=高/橙=中/绿=低），优先级标签，截止日期智能显示（今天/明天/昨天/MM/dd），备注预览。
*   **勾选完成**：点击复选框切换完成状态，已完成项目灰色删除线显示。
*   **滑动删除**：左滑弹出确认对话框，支持 Snackbar 撤销删除。
*   **新增/编辑待办**：FAB 或点击卡片跳转 `EditTodoActivity`，支持设置标题、截止日期/时间、备注、优先级（低/中/高）。

#### 4.2.2 关键技术点实现
*   **Activity 间数据传递**：通过 `Intent` 携带 `todo_id` Bundle 数据，在 `EditTodoActivity` 中判断新建/编辑模式。
*   **列表动画与手势**：`ItemTouchHelper.SimpleCallback` 结合 `RecyclerView` 实现左滑删除。
*   **优先级别 UI**：三级 Chip 选择器，切换时变更背景样式（chip_selected_bg / chip_default_bg）。

### 4.3 核心板块三：番茄钟 (Pomodoro Timer)

#### 4.3.1 界面交互说明
*   **状态展示**：顶部状态栏显示当前倒计时、阶段标签（工作/短休息/长休息）。运行中显示暂停/停止按钮，空闲时显示默认时长。
*   **全屏计时**：点击状态栏跳转 `PomodoroTimerActivity`，圆形进度条 + 大字号等宽倒计时数字 + 阶段标签。
*   **控制项**：单一按钮循环开始→暂停→继续→暂停，停止按钮退出计时。
*   **任务列表**：支持添加番茄钟任务，预设时长芯片（25/30/45/60 分钟）选中高亮（黄色填充+深黄描边），或自定义分钟数。
*   **设置页**：配置工作时长、短休息时长、长休息时长、长休息间隔周期数。

#### 4.3.2 关键技术点实现
*   **前台服务 (Foreground Service)**：`PomodoroService` 使用 `CountDownTimer` 驱动倒计时，`START_STICKY` 确保被杀后自动重启恢复状态。
*   **阶段循环**：完成一个工作周期后进入短休息；完成 N 个工作周期（可配置）后进入长休息；休息结束后回到工作。
*   **广播通信**：通过 6 种自定义广播（TICK / PAUSED / RESUMED / STOPPED / PHASE_CHANGED / FINISHED）在 Service 与 UI 组件间同步状态。
*   **通知栏同步**：前台通知显示倒计时进度条 + 暂停/停止操作按钮，通过 `NotificationActionReceiver` 处理通知点击。
*   **状态持久化**：所有计时状态写入 SharedPreferences（running / paused / phase / total / remaining / length），进程死亡后自动恢复。
*   **全局计数**：完成一个工作周期递增 `pomodoro_count`，在"我的"页面展示。

### 4.4 核心板块四：AI 智能助手 (AI Assistant)

#### 4.4.1 界面交互说明
*   **对话界面**：气泡式聊天界面（发送/接收双样式），底部输入框 + 发送按钮，支持清空对话、长按删除单条消息。
*   **自然语言操作**：用户可使用自然语言创建/修改/删除/完成待办、习惯和番茄钟任务，AI 自动执行并反馈结果。
*   **API 配置**：首次使用提示配置 DeepSeek API Key，支持随时在"我的"→"AI 设置"中修改。

#### 4.4.2 关键技术点实现
*   **Retrofit API 集成**：`ChatApi`（POST `/chat/completions`，DeepSeek 兼容接口）。
*   **命令协议 (CMD Protocol)**：AI 回复中嵌入 `[CMD]{"action":"...","参数":"值"}[/CMD]` JSON 块，由 `CommandParser` 提取并执行。支持 12 种操作。
*   **指令执行器**：`CommandParser.process()` 扫描 AI 回复中的所有命令块，提取 JSON 参数后通过 DAO 层操作数据库，返回执行日志。
*   **周一至周日解析**：`parseDays()` 支持数字串（`"124"`）与中文格式（`"周一、二、四"`）的星期解析。
*   **对话管理**：消息持久化到 `chat_message` 表，按 `session_id` 分用户隔离。完整对话上下文随每次 API 请求发送。
*   **重试机制**：若 AI 回复不含命令但用户意图为操作请求，自动重试一次并附加强提醒。
*   **本地推荐引擎**：`RecommendationEngine` 基于时间槽匹配（权重 2.0）+ 历史完成率（权重 3.0）+ 今日未打卡奖励（权重 1.0）的规则评分，无需远程 API。

#### 4.4.3 命令列表

| 命令 | 参数 | 说明 |
|:---|:---|:---|
| `create_todo` | title, note(可选), priority(1-3), due(yyyy-MM-dd HH:mm) | 创建待办 |
| `delete_todo` | title(关键词) 或 id | 删除待办 |
| `complete_todo` | title 或 id | 完成待办 |
| `update_todo` | title(匹配用) 或 id, new_title(新标题,可选), note, priority, due | 修改待办 |
| `list_todos` | 无 | 列出所有待办 |
| `create_habit` | name, freq(每日/每周), days(如"124"), time(HH:mm), desc(可选) | 创建习惯 |
| `delete_habit` | name 或 id | 删除习惯 |
| `update_habit` | name(匹配用) 或 id, new_name, desc, icon, color, freq, time, days | 修改习惯 |
| `list_habits` | 无 | 列出所有习惯 |
| `create_pomo` | name, minutes(如25) | 创建番茄钟任务 |
| `delete_pomo` | name 或 id | 删除番茄钟任务 |
| `update_pomo` | name(匹配用) 或 id, new_name, icon, minutes | 修改番茄钟任务 |
| `list_pomos` | 无 | 列出所有番茄钟任务 |

### 4.5 核心板块五：我的 (Mine & Settings)

#### 4.5.1 界面交互说明
*   **个人信息区**：头像（可换 40 款 emoji 头像）、登录状态（点击登录/登出）。
*   **数据统计面板**：三格展示习惯数、待办数、番茄专注完成次数。
*   **位置卡片**：显示当前地址、经纬度、精度。App 启动时自动在后台获取位置，切换到"我的"时即显示最新结果，无需手动刷新。
*   **设置列表**：AI 设置（API Key 配置）、数据备份管理、应用权限入口。

#### 4.5.2 关键技术点实现

| 设置子项 | 对应功能描述 | 技术实现路径 |
|:---|:---|:---|
| **登录/注册** | 本地账号密码认证，支持记住密码 | SHA-256 哈希存储密码；Android Keystore AES-256/GCM 加密记住的密码；SharedPreferences 存储登录历史 |
| **头像选择** | 40 款 emoji 头像可选 | GridView 网格选择器，SharedPreferences 按用户独立存储 |
| **AI 设置** | DeepSeek API Key 配置 | SharedPreferences 存储 API Key；ChatClient 支持热切换 |
| **数据备份** | 本地数据导出与恢复 | `BackupManager`：将 SQLite 数据导出为本地 `.json` 备份文件（双存储：应用私有目录 + 公共 Downloads），支持从文件或 URI 恢复（追加式导入） |
| **密码锁** | 启动隐私安全保障 | SharedPreferences 存储 SHA-256 密码哈希值。开启后每次冷启动弹出 `LockActivity` |
| **每日提醒** | 定时推送提醒打卡 | 结合 `AlarmManager` 与 `DailyReminderReceiver`，在用户设定时间发送本地通知 |
| **位置服务** | 获取当前位置与地址 | `LocationHelper`（FusedLocationProviderClient 单例）：支持单次高精度定位（15s 超时 + 后备 last known）、启动时后台自动定位、LiveData 观察、反向地理编码（Geocoder） |
| **应用权限** | 跳转系统权限设置页 | `ACTION_APPLICATION_DETAILS_SETTINGS` 打开本应用权限管理页 |

#### 4.5.3 个人中心统计逻辑
*   **习惯数**：`habitDao.getByUser(userId).size()`
*   **待办数**：`todoDao.getByUser(userId).size()`
*   **专注完成数**：`pomodoroSessionDao.getTotalCompletedCountForUser(userId)`

---

## 5. 特色与高级功能设计

### 5.1 强制下线功能 (Force Offline Receiver)
*   **业务场景**：用户多端登录冲突或主动选择安全退出时，需要强制结束当前会话。
*   **技术实现**：`ForceOfflineReceiver` 动态注册到 `MainActivity`，监听自定义广播 `com.example.todolist.FORCE_OFFLINE`。收到广播后清除 `app_unlocked` 标记，弹出不可取消的 `AlertDialog` 提示"您的账号已在其他设备登录或已失效，请重新登录"。

### 5.2 跨程序数据共享 (Content Provider)
*   **业务场景**：模拟外部应用或桌面小部件需要获取待办数据。
*   **技术实现**：自定义 `TodoProvider`（authority: `com.example.todolist.provider`），在 `AndroidManifest.xml` 中注册。提供两个 URI：
    *   `/todos`：返回 `MatrixCursor`（id, title, is_completed 列）
    *   `/todos/count`：返回待办总数。

### 5.3 AI 对话与自然语言操作
*   **业务场景**：用户通过自然语言快速管理待办、习惯和番茄钟任务，无需手动操作 UI。
*   **技术实现**：DeepSeek Chat API 集成，通过系统提示注入 `[CMD]` 协议规范。`CommandParser` 本地解析并执行 AI 回复中的命令（12 种操作：创建/删除/完成/修改 × 三种实体），通过 DAO 层操作 Room 数据库，执行结果内联反馈至对话历史。

### 5.4 本地智能推荐引擎 (Local Recommendation Engine)
*   **业务场景**：根据用户当前时间段和历史打卡行为，个性化推荐适合当前进行的习惯。
*   **技术实现**：基于规则的轻量级评分系统：
    *   **时间槽匹配**（权重 2.0）：通过关键词字典将习惯名映射到早/午/晚时段
    *   **历史完成率**（权重 3.0）：最近 7 天打卡完成比例
    *   **今日未打卡奖励**（权重 1.0）：当天尚未打卡加 1.0 分
    *   综合得分排序，返回 Top-N 推荐。

### 5.5 启动页与励志语录 (Splash Screen)
*   **业务场景**：应用冷启动展示过渡动画与一条随机励志语录。
*   **技术实现**：`SplashActivity` 显示 2500ms，淡入动画（600ms ObjectAnimator）。优先从 `api.quotable.io` 获取英文语录（OkHttp，5s 超时），失败则使用本地预置的 15 条中文励志短语。

### 5.6 番茄钟前台服务与三段式循环
*   **业务场景**：确保倒计时在应用进入后台或锁屏时不中断，通知栏同步进度。
*   **技术实现**：`PomodoroService` 前台服务（`specialUse` 类型），`CountDownTimer` 1 秒间隔驱动。完成一个工作周期后自动进入短休息/长休息，休息结束回到工作。`START_STICKY` 确保进程死亡后自动恢复。

### 5.7 位置服务与后台自动定位
*   **业务场景**：App 启动时自动在后台获取当前位置，用户切换到"我的"页面时无需等待即看到地址信息。
*   **技术实现**：`MainActivity` 启动时自动调用 `requestSingleLocation()`（如已授权）。`LocationHelper` 通过 LiveData 广播结果，`MineFragment` 观察并显示地址、坐标、精度。15 秒超时自动降级到上次已知位置。

---

## 6. 数据结构与持久化设计

应用涉及的数据表结构共七张核心表：

### 6.1 待办数据表 (todo_item)
| 字段名 | 类型 | 约束 | 说明 |
|:---|:---|:---|:---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 主键 ID |
| `title` | TEXT | NOT NULL | 待办名称 |
| `note` | TEXT | | 备注详细内容 |
| `due_date` | LONG | | 截止日期时间戳 |
| `is_completed` | INTEGER | DEFAULT 0 (0/1) | 是否已完成 |
| `priority` | INTEGER | DEFAULT 1 | 优先级 (1-低, 2-中, 3-高) |
| `user_id` | TEXT | DEFAULT '' | 所属用户（空串为旧数据兼容） |

### 6.2 习惯数据表 (habit_item)
| 字段名 | 类型 | 约束 | 说明 |
|:---|:---|:---|:---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 主键 ID |
| `name` | TEXT | NOT NULL | 习惯名称 |
| `description` | TEXT | | 描述 |
| `icon_res` | TEXT | | 图标/emoji 标识符 |
| `frequency` | TEXT | | 频率标签（"每日"/"每周"） |
| `color` | TEXT | | 颜色 hex 值 |
| `create_time` | LONG | | 创建时间 |
| `schedule_config` | TEXT | | JSON 调度配置（见下文） |
| `user_id` | TEXT | DEFAULT '' | 所属用户 |

**schedule_config JSON 格式：**
*   `{"mode":"daily"}` — 每日，任意时间
*   `{"mode":"daily","time":480}` — 每日 08:00
*   `{"mode":"weekly","days":"124","time":540}` — 周一/二/四 09:00（1=周一...7=周日）
*   `{"mode":"specific","date":1718409600000,"time":840}` — 指定日期 14:00

### 6.3 习惯打卡表 (habit_check)
| 字段名 | 类型 | 约束 | 说明 |
|:---|:---|:---|:---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 主键 ID |
| `habit_id` | LONG | NOT NULL | 关联习惯 ID |
| `date_stamp` | LONG | NOT NULL | 日期标记 (yyyyMMdd) |
| `checked` | INTEGER | DEFAULT 0 (0/1) | 打卡状态 |
| `user_id` | TEXT | DEFAULT '' | 所属用户 |

唯一索引：`(habit_id, date_stamp, user_id)`

### 6.4 习惯打卡表（新版）(habit_checkin)
| 字段名 | 类型 | 约束 | 说明 |
|:---|:---|:---|:---|
| `id` | LONG | PRIMARY KEY AUTOINCREMENT | 主键 ID |
| `habitId` | LONG | NOT NULL | 关联习惯 ID |
| `dateStamp` | LONG | NOT NULL | 日期标记 |
| `isCompleted` | BOOLEAN | DEFAULT true | 完成状态 |

唯一索引：`(habitId, dateStamp)`

### 6.5 番茄钟任务表 (pomodoro_task)
| 字段名 | 类型 | 约束 | 说明 |
|:---|:---|:---|:---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 主键 ID |
| `name` | TEXT | | 任务名称 |
| `icon` | TEXT | | 图标/emoji |
| `duration_minutes` | INTEGER | | 时长（分钟） |
| `create_time` | LONG | | 创建时间 |
| `user_id` | TEXT | DEFAULT '' | 所属用户 |

### 6.6 番茄钟会话表 (pomodoro_session)
| 字段名 | 类型 | 约束 | 说明 |
|:---|:---|:---|:---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 主键 ID |
| `task_id` | LONG | FK → pomodoro_task(id) ON DELETE CASCADE | 关联任务 |
| `start_time` | LONG | | 开始时间戳 |
| `end_time` | LONG | NULLABLE | 结束时间戳（null = 进行中） |
| `completed` | INTEGER | DEFAULT 0 (0/1) | 是否完成 |
| `user_id` | TEXT | DEFAULT '' | 所属用户 |

### 6.7 聊天消息表 (chat_message)
| 字段名 | 类型 | 约束 | 说明 |
|:---|:---|:---|:---|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | 主键 ID |
| `content` | TEXT | NOT NULL | 消息内容 |
| `type` | INTEGER | | 0=接收到(AI), 1=发送(用户) |
| `timestamp` | LONG | | 时间戳 |
| `session_id` | TEXT | NOT NULL | 会话标识（格式：`ai_chat_<userId>`） |

### 6.8 其他持久化数据

| 数据 | 存储方式 | 说明 |
|:---|:---|:---|
| 用户密码 | SharedPreferences (`user_<name>_password`) | SHA-256 哈希值 |
| 记住密码 | SharedPreferences (`rem_pwd_<name>`) | AES-256/GCM 加密（Android Keystore） |
| 登录历史 | SharedPreferences (`login_history`) | 去重 LinkedHashSet，最多 4 个最近用户 |
| 当前用户 | SharedPreferences (`logged_in_user`) | 登录用户名 |
| 应用锁开关 | SharedPreferences (`password_hash`, `app_unlocked`) | 密码哈希 + 解锁状态 |
| 番茄钟配置 | SharedPreferences (`pomodoro_length` 等) | 工作/休息时长、周期数、计数 |
| AI API 配置 | SharedPreferences (`deepseek_api_key`, `deepseek_api_url`) | DeepSeek API 密钥和 URL |
| 头像 | SharedPreferences (`avatar_<username>`) | emoji 字符 |
| 每日提醒 | SharedPreferences (`reminder_*`) | 提醒开关与时间 |
| 备份文件 | File Storage (`files/backups/` + 公共 Downloads) | JSON 格式 |

---

## 7. 非功能性需求与约束

### 7.1 界面规范与自适应
*   **视觉风格**：整体采用 Material Design 3 风格（`Theme.MaterialComponents.Light.NoActionBar`），主色选用温暖柔和的黄色系（#FFD54F / #FFC107），减少高饱和度色彩造成的视觉疲劳。
*   **自适应布局**：使用 `RecyclerView` 和卡片式布局，确保在不同长宽比的安卓手机屏幕上界面元素不发生重叠或变形。
*   **组件统一**：圆角卡片（12dp radius）、优先级色带、矢量图标（VectorDrawable 24dp）、底部导航带标签。

### 7.2 安全性与健壮性
*   **权限管理**：运行时动态申请位置、通知、存储、录音权限。对用户拒绝权限的情况进行合理降级处理。
*   **数据异常防护**：所有数据库读写操作在后台线程（`ExecutorService` 单线程池）中进行，避免界面卡死（ANR）。
*   **密码安全**：
    *   登录密码使用 SHA-256 哈希存储。
    *   记住密码使用 Android Keystore 硬件级 AES-256/GCM 加密（密钥不离开设备，卸载后销毁）。
*   **状态恢复**：番茄钟服务支持进程死亡后自动恢复（`START_STICKY` + SharedPreferences 状态持久化）。
*   **多用户隔离**：所有数据表带 `user_id` 字段，DAOs 查询按用户过滤（兼容旧数据 `user_id=''`）。
*   **防御性编程**：所有核心初始化操作包裹 try-catch（如 `LocalDate.now()` 有后备值），防止意外崩溃。

### 7.3 性能与兼容性
*   **最低 SDK 版本**：API 24 (Android 7.0)
*   **目标 SDK 版本**：API 36
*   **编译 SDK 版本**：API 36
*   **Java 兼容性**：Java 11（启用 coreLibraryDesugaring 以支持 `java.time`）
*   **线程安全**：ChatClient 双检锁单例；AppDatabase 双检锁单例；LocationHelper 线程安全单例。
*   **超时设置**：AI API 30-60s 超时；定位请求 15s 超时；启动页语录请求 5s 超时。

---

## 8. UI 导航结构

```
SplashActivity (启动页, 2.5s)
       │
       ▼
   [密码锁检查] ──yes──▶ LockActivity
       │no                  │
       ▼                    ▼
   MainActivity ◀───────────┘ (RESULT_OK)
       │
       ├─ HabitFragment ──▶ EditHabitActivity
       ├─ TodoFragment ───▶ EditTodoActivity
       ├─ PomodoroFragment ──▶ PomodoroTimerActivity
       │                    └─▶ PomodoroSettingsActivity
       ├─ DiscoverFragment (ChatFragment 子 Fragment)
       └─ MineFragment ────▶ LoginActivity
```

---

## 9. AndroidManifest 组件声明

| 类别 | 组件名 | 导出 | 说明 |
|:---|:---|:---|:---|
| Activity | `.SplashActivity` | 是 (LAUNCHER) | 启动页 |
| Activity | `.MainActivity` | 否 | 主页面 |
| Activity | `.LoginActivity` | 否 | 登录注册 |
| Activity | `.LockActivity` | 否 | 密码锁 |
| Activity | `.EditTodoActivity` | 否 | 编辑待办 |
| Activity | `.EditHabitActivity` | 否 | 编辑习惯 |
| Activity | `.PomodoroTimerActivity` | 否 | 全屏番茄钟 |
| Activity | `.PomodoroSettingsActivity` | 否 | 番茄钟设置 |
| Service | `.service.PomodoroService` | 否 (specialUse) | 番茄钟前台服务 |
| Receiver | `.receiver.NotificationActionReceiver` | 否 | 通知操作处理 |
| Provider | `.provider.TodoProvider` | 否 | 跨进程数据共享 |

### 声明的权限
`INTERNET` / `WRITE_EXTERNAL_STORAGE` (≤28) / `READ_EXTERNAL_STORAGE` (≤32) / `POST_NOTIFICATIONS` / `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` / `SCHEDULE_EXACT_ALARM` / `VIBRATE` / `RECORD_AUDIO` / `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`

---

## 10. 依赖库与技术栈

| 类别 | 技术 |
|:---|:---|
| **语言** | Java, Kotlin |
| **UI 框架** | Jetpack Compose + Material 3, ViewBinding, ConstraintLayout, RecyclerView |
| **架构** | Room, SharedPreferences, ViewPager2, Fragment, LiveData (Location) |
| **后台** | Foreground Service, AlarmManager, BroadcastReceiver, CountDownTimer |
| **网络** | Retrofit 2 + OkHttp + Gson |
| **安全** | Android Keystore (AES-256/GCM), SHA-256 |
| **位置** | Google Play Services FusedLocationProviderClient, Geocoder |
| **构建** | Gradle (Kotlin DSL) + Version Catalog (.toml) |
