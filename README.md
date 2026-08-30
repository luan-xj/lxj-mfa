# LXJ-MFA

一款原生 Kotlin 实现的安卓双因素认证（MFA）应用，支持多类型、多算法、本地加密存储与 Git 备份同步。

## 功能特性

- 多账号管理，支持 **TOTP / HOTP / MOTP / STEAM** 四种类型
- 算法支持 **SHA1 / SHA256 / SHA512**
- 扫码导入、账号标签、主界面模糊搜索（issuer/label/tag/type/algorithm）
- 本地加密存储（主密码 `PBKDF2WithHmacSHA256` + Android Keystore + EncryptedSharedPreferences）
- 启动指纹 / 密码锁
- Git 加密备份与多设备同步（JGit + HTTPS Token）
- 内置使用说明（设置页，署名 —— LXJ）

## 环境要求

- **JDK 17**（如 Microsoft OpenJDK 17）
- **Android SDK**：Android 14 / API 34，build-tools 34.0.0
- **Gradle 8.9**（可用仓库自带 wrapper 自动下载，或用本机已装的 Gradle）

## 从源码构建

1. 安装 JDK 17 与 Android SDK（需包含 `platforms;android-34`、`build-tools;34.0.0`）。
2. 配置 SDK 路径（二选一）：
   - 创建 `local.properties`，写入 `sdk.dir=你的SDK路径`（例如 `D:/Android/Sdk`）；或
   - 设置环境变量 `ANDROID_HOME` 指向 SDK 目录。
3. 构建：
   - 使用仓库自带的 wrapper（首次会自动下载 Gradle 8.9，需联网）：
     ```bash
     ./gradlew assembleDebug      # Linux / macOS
     gradlew.bat assembleDebug    # Windows
     ```
   - 或使用本机已安装的 Gradle 8.9：
     ```bash
     gradle assembleDebug
     ```
4. 产物：`app/build/outputs/apk/debug/LXJ-MFA-v1.0.2.apk`

> 安装提示：覆盖安装不会刷新 launcher 图标（Android 缓存所致），需卸载重装或清除桌面缓存后才会显示新图标。

## 使用简介

应用内「设置 → 使用说明」有完整介绍；仓库内的 [`LXJ-MFA开发记录.md`](LXJ-MFA开发记录.md) 记录了完整的开发历程、模块设计与踩坑经验。

## 许可证

[MIT](LICENSE) © 2026 LXJ
