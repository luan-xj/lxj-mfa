# LXJ-MFA 安卓应用 · 开发记录与交付存档

> 整理时间：2026-08-30
> 包名：`com.lxj.mfa`
> 工程路径：`D:\WorkBuddy\Projects\LXJ-MFA\`
> 当前版本：`v1.0.2`（versionCode=2）

---

## 一、项目概述

LXJ-MFA 是一款**原生 Kotlin** 实现的双因素认证（MFA）安卓应用，用于替代各类商用 TOTP 工具。

核心能力：
- 多账号管理，支持 **TOTP / HOTP / MOTP / STEAM** 四种类型
- 算法支持 **SHA1 / SHA256 / SHA512**
- 本地加密存储（主密码 + Android Keystore + EncryptedSharedPreferences）
- 启动指纹/密码锁
- 通过 Git（JGit + HTTPS Token）做加密备份与多设备同步
- 扫码导入、模糊搜索、使用说明书（署名 —— LXJ）

---

## 二、技术栈与编译环境

| 项目 | 版本 / 路径 |
|------|-------------|
| 语言 | Kotlin 1.9.24 |
| AGP | 8.5.2 |
| Gradle | 8.9（`D:\WorkBuddy\tools\gradle-8.9`） |
| compileSdk / targetSdk | 34 |
| minSdk | 26（Android 8.0） |
| JDK | 17（`C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot`） |
| Android SDK | `D:\Android\Sdk` |
| 数据库 | Room v1 → v2 |

**编译命令（git-bash 风格路径）**

```bash
export JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.20.101-hotspot"
export ANDROID_HOME="D:/Android/Sdk"
export GRADLE_USER_HOME="C:/Users/luan-/.gradle"
/d/WorkBuddy/tools/gradle-8.9/bin/gradle assembleDebug --no-daemon
```

> 说明：路径空格用 `/c/` 而非 `C:/`，否则与 PATH 冒号冲突。

---

## 三、功能演进（按需求轮次）

### 第 1 轮 · 基础架构落地
- 确定选型：HTTPS+Token、本地加密存储、TOTP+扫码导入、原生 Kotlin 工程。
- 实现：多账号 MFA（TOTP/SHA1-512）、本地 AES(Keystore)+EncryptedSharedPreferences 加密、Git 同步（JGit，备份文件名 `lxj-mfa-backup.json`，可选密码加密）、启动指纹/密码锁、自适应清新图标、状态栏避让。
- 产物：`app-debug.apk`（8.6MB，aapt2 校验通过）。

### 第 2 轮 · 类型/算法/搜索/标签
- 后台锁定时长选择由 Spinner 改为**卡片点击弹单选框**，更明显。
- 账号类型扩展：**TOTP / HOTP / MOTP / STEAM**。
- 算法扩展：**SHA1 / SHA256 / SHA512**。
- 账号新增**标签（tag）**属性。
- 主界面新增**模糊搜索**（匹配 issuer / label / tag / type / algorithm）。
- 图标改为盾牌 + MFA 字样。
- 数据库 `version 1 → 2`，新增 `type` / `tag` / `counter` 三列，写 `Migration(1,2)` 加列，**不丢旧数据**。
- 新增 `totp/Otp.kt` 统一分发：HOTP(RFC4226+计数器，复制后自增)、STEAM(Steam 字母表 5 位)、MOTP(MD5，密钥格式 `十六进制:PIN`)、TOTP 走原有 Totp。
- 产物 9.7MB。

### 第 3 轮 · 图标红字 / 立即锁定 / Git 修复 / 主密码
- 图标 MFA 字样由绿改红 `#E53935`（白盾+红字）。
- 主界面新增「立即锁定」按钮（toolbar 菜单 `action_lock` + `ic_lock.xml`），点后 `AppState.unlocked=false` 并启动 `LockActivity`。
- **修复 Git 同步拉取失败根因**：加密备份 salt 原本只存在本机 `EncryptedSharedPreferences`，换设备/重装后本地无 salt → 解密失败。改为把 salt 直接写入备份文件 `lxj-mfa-backup.json` 的 `salt` 字段，pull 时从文件读；旧备份无 salt 则回退本地。密码错误时明确抛「同步密码错误，无法解密备份文件」。
  - 注意：「加密密码」指**同步密码**，独立于本地主密码，二者别混。
  - 修复前已 push 的无 salt 旧备份，在新设备仍无法解；需在原设备再 push 一次（携 salt）后，同密码任意设备可拉。
- 主密码存储：用户要求 SHA256；实际已用 `PBKDF2WithHmacSHA256`（12 万次迭代 + 随机盐），属 SHA256 族且更安全，未作降级。
- 产物 9.7MB。

### 第 4 轮 · 对比度 / 说明书
- UI 对比度修复：搜索框、设置页 Git 三个输入框显式设 `textColor=#163A2F`、字号 15sp，避免输入文字发灰。
- 仓库框下方加说明「填写 https:// 开头的仓库地址…」。
- 新增「使用说明 / 介绍」页：`HelpActivity.kt` + `activity_help.xml`（ScrollView，含介绍、添加账号、解锁锁定、Git 同步、安全说明五段）；入口两处——主界面 toolbar 菜单 `action_help` 与设置页底部按钮 `btn_help`。
- 踩坑：`SettingsActivity` 缺 `import android.content.Intent` → 编译报错，已补。
- 产物 9.7MB。

### 第 5 轮 · APK 文件名版本化
- 用户要求：别用 `app-debug.apk`，用版本号区分。
- 实现：`versionName=1.0.0`；注册 `renameDebugApk` / `renameReleaseApk` 任务，把 `app-debug.apk` 重命名为 `LXJ-MFA-v1.0.0.apk`，`afterEvaluate` 内 `assemble` 任务 `finalizedBy` 重命名。
- 踩坑：AGP 8 的 `applicationVariants` 已无 `outputFileName`，改用「编译后重命名任务」最稳。
- 产物：`LXJ-MFA-v1.0.0.apk`（8.3MB）。

### 第 6 轮 · 图标强化 / 实时预览 / 移除倒计时 / 锁常驻 / 署名
- 图标：白盾+红MFA 加深绿描边并加粗 MFA（增强辨识）。
- 添加/编辑对话框内实时预览：输入后显示「当前验证码」「下一验证码」「剩余 Xs」（1s 定时器刷新），HOTP 显示「计数器型(无下一码)」。
- 首页卡片显示「下一 123 456」小字（HOTP 隐藏）；`Otp.kt` 新增 `codeAt(a, secret, timeMillis)`，steam/motp 支持 next，`code()` 改调 `codeAt(..., now)`。
- 移除顶部全局剩余时间倒计时（保留卡片进度条）。
- toolbar：立即锁定 `showAsAction="always"`，使用说明移出 toolbar（仅设置页入口）；帮助页底部署名「—— LXJ」。
- 编译通过（仅 IntentIntegrator 弃用警告）。

### 第 7 轮 · 新图标 / 升版本 / next 文案 / 卡片紧凑（v1.0.2）
- 图标：放弃盾牌+红MFA，重画为**白底绿锁孔挂锁**造型（`ic_launcher_foreground.xml`）。
- 升版本：versionCode=2，versionName=`1.0.2`。
- 验证码「下一」文案改 `next`（`preview_next="next %1$s"`）。
- 账号卡片紧凑化（`item_account.xml` padding 14→10、字号缩小、操作按钮 40→32dp、`tv_next` 显示 "next xxx"），一页显示更多。
- **环境坑解决**：用户重启电脑后 Defender 独占的 `.lock` 释放；本次用 `--no-daemon` 编译，`BUILD SUCCESSFUL in 32s`。
- 产物：`LXJ-MFA-v1.0.2.apk`（8.7MB）。

---

## 四、核心模块说明

### 4.1 数据库（Room）
- `Account` 实体字段：`id`、`issuer`、`label`、`secret`、`type`(默认 TOTP)、`algorithm`、`digits`、`period`、`tag`、`counter`、`createdAt`。
- 版本 v1→v2 迁移：`Migration(1,2)` 加 `type` / `tag` / `counter` 列。

### 4.2 OTP 分发器（`totp/Otp.kt`）
| 类型 | 标准 | 说明 |
|------|------|------|
| TOTP | RFC6238 | SHA1/256/512，时间型，支持 `codeAt` 取下一周期 |
| HOTP | RFC4226 | 计数器型，复制后自增 |
| STEAM | Steam | 5 位字母表（无 `/` 等易混字符） |
| MOTP | Mobile-OTP | MD5，密钥格式 `十六进制:PIN` |

### 4.3 加密与安全
- 主密码：`PBKDF2WithHmacSHA256`，12 万次迭代 + 随机盐（属 SHA256 族）。
- `EncryptedSharedPreferences` + Android `MasterKeys`（AES256_GCM_SPEC）存敏感配置。
- Git 备份：AES-GCM 加密（密钥由同步密码派生），**salt 写入备份 JSON** 随文件携带，换设备可恢复。

### 4.4 Git 同步（`git/GitSync.kt`）
- JGit HTTPS + Token。
- push 写入 `lxj-mfa-backup.json`（含 `salt` 字段）；pull 从文件读 salt 解密。
- 注意：**空 Git 仓库首次 clone 会失败**（JGit 限制），需先在远端建好带初始提交的分支。

---

## 五、踩坑与解决方案（汇总）

| # | 问题 | 解决 |
|---|------|------|
| 1 | `security-crypto:1.0.0` 无 `MasterKey` | 用 `MasterKeys.getOrCreate(AES256_GCM_SPEC)` + `EncryptedSharedPreferences.create(...)` |
| 2 | `zxing-android-embedded` 的 `IntentIntegrator` 包名 | 在 `com.google.zxing.integration.android`；`setCaptureActivity(CaptureActivity::class.java)` |
| 3 | `biometric:1.2.0` 不存在 | 改用 `1.1.0` |
| 4 | 清单 `CaptureActivity` 与库合并冲突 | `tools:replace="android:screenOrientation,android:theme"` |
| 5 | AGP 8 `outputFileName` 失效 | 改用 assemble 后「重命名任务」方案 |
| 6 | JVM C2 JIT 崩溃（`asm.ClassReader` EXCEPTION_ACCESS_VIOLATION） | `gradle.properties` 加 `-XX:TieredStopAtLevel=1`（gradle 与 kotlin daemon 各一份）；再崩可用 `-Xint` |
| 7 | **Windows Defender 锁死 `.gradle` 的 `.lock` 文件**（Access denied，无管理员删不掉） | 重启电脑释放句柄 + 编译加 `--no-daemon` 规避 daemon 残留锁；教训：**勿用 `Stop-Process -Force` 强杀 Gradle/Kotlin daemon，应用 `gradle --stop`** |

---

## 六、版本产物清单

| 版本 | 文件名 | 大小 | 路径 |
|------|--------|------|------|
| v1.0.0 | `LXJ-MFA-v1.0.0.apk` | 8.3MB | `app/build/outputs/apk/debug/` |
| v1.0.2 | `LXJ-MFA-v1.0.2.apk` | 8.7MB | `app/build/outputs/apk/debug/` |

> 最新可用：**`D:\WorkBuddy\Projects\LXJ-MFA\app\build\outputs\apk\debug\LXJ-MFA-v1.0.2.apk`**

---

## 七、安装与分发注意事项

1. **覆盖安装不刷新 launcher 图标**（Android 缓存旧图标）：要看到新锁形图标，需**卸载重装**或清桌面/luncher 缓存。
2. 当前两个版本均为 **debug 签名**，覆盖安装同签名不冲突；若报签名冲突需先卸载（Room 数据会丢，建议先 Git 加密同步）。
3. **正式 release 包**尚未生成（用户待确认是否需要），release 包可带正式签名直接对外分发安装。
4. Git 同步首次使用：先在远端建好带初始提交的分支，再填仓库/分支/Token。

---

## 八、后续可选项

- [ ] 生成正式签名 release 包（`LXJ-MFA-v1.0.2-release.apk`）
- [ ] 多设备同步的「首次空仓库 clone」引导优化
- [ ] 图标在小尺寸下的可读性再校验
