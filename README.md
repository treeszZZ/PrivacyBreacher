# 好梦 | Sleep—registrar

> 一个简洁、隐私安全的安卓睡眠记录工具。自动记录锁屏时间，帮助你追踪入睡时刻，培养规律作息。

---

## 功能一览

- 自动记录：后台监听手机锁屏事件，自动记录每次锁屏时间
- 待确认列表：锁屏后记录出现在待确认列表，由你确认哪一次是真实入睡时间
- 已确认记录：所有已确认的入睡时间以列表或日历视图展示
- 日历视图：方块日历，按颜色直观展示每日入睡时间
- 标色阈值：自定义阈值时间，日历中早于等于阈值为蓝色，晚于阈值为白色，无记录为灰色
- 撤回功能：误确认后可撤回，记录回到待确认列表
- 数据导出：一键导出已确认记录为 CSV 文件，方便备份与分析
- 隐私安全：所有数据仅保存在手机本地，不上传任何服务器

---

## 技术栈

| 技术 | 用途 |
| :--- | :--- |
| Android SDK (Java) | 原生安卓开发 |
| SQLite | 本地数据持久化 |
| SharedPreferences | 用户设置存储 |
| BroadcastReceiver | 监听系统锁屏广播 |
| Foreground Service | 后台持续运行 |
| RecyclerView + ViewPager2 | 列表与日历视图切换 |
| TabLayout | 视图切换指示器 |
| AlertDialog | 确认/撤回弹窗 |
| FileProvider | 导出文件分享 |

---

## 主要界面

### 首页（待确认列表）
- 显示当前睡眠区间
- 显示今日待确认记录数量
- 列表展示所有待确认记录，每条记录右侧有确认按钮

### 已确认记录
- 列表视图：按日期倒序/正序排列，每条记录右侧有撤回按钮
- 日历视图：方块日历，通过颜色直观展示每日入睡时间

### 设置页面
- 修改睡眠区间
- 修改标色阈值（日历颜色分界线）
- 导出数据为 CSV 文件

---

## 本地运行

### 前置条件
- Android Studio 2023.1+
- JDK 11 或 17
- Android SDK (compileSdk 34)

### 使用方式
克隆项目-生成 APK-手机下载

### 项目结构（精简）
text
app/src/main/
├── java/io/nandandesai/privacybreacher/
│   ├── MainActivity.java              # 主入口
│   ├── HomeFragment.java              # 首页（含待确认 + 已确认记录）
│   ├── SettingsFragment.java          # 设置页面
│   ├── AddFragment.java               # 手动添加记录
│   ├── DataBaseHelper.java            # 数据库操作
│   ├── EventReceiver.java             # 锁屏广播接收
│   ├── PrivacyBreacherService.java    # 后台服务
│   ├── ConfirmAdapter.java            # 待确认列表适配器
│   ├── ConfirmedAdapter.java          # 已确认列表适配器
│   ├── ConfirmedRecord.java           # 已确认记录实体
│   └── ConfirmRecord.java             # 待确认记录实体
├── res/
│   ├── layout/                        # 所有布局文件
│   ├── drawable/                      # 图标与样式资源
│   ├── values/                        # 颜色、字符串、样式
│   └── xml/                           # FileProvider 配置
└── AndroidManifest.xml                # 清单文件

---

## 使用说明
首次使用：打开 App，进入设置页面，调整睡眠区间和标色阈值。
日常使用：正常锁屏即可，App 会在后台自动记录。
确认记录：第二天打开 App，在首页待确认列表中点击确认即可。
查看统计：切换到已确认记录的列表或日历视图。
撤回误操作：在已确认列表中点击撤回，记录回到待确认列表。
导出数据：设置页面点击导出数据，生成 CSV 文件并分享。

### 致谢
本项目基于 PrivacyBreacher 修改与扩展，感谢原作者 Nandan Desai 的开源工作。本版本在原项目基础上新增了睡眠追踪的核心功能模块，并重构了界面与交互逻辑。

### 许可证
本项目采用 MIT 许可证，详情见 LICENSE 文件。
