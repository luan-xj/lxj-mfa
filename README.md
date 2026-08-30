# LXJ-MFA

一款原生 Kotlin 实现的 Android 双因素认证（2FA / MFA）应用，支持多种动态口令算法、本地加密存储与 Git 备份同步，并内置应用内更新与崩溃日志反馈能力。

## 功能特性

| 类别 | 说明 |
| --- | --- |
| 账号类型 | TOTP / HOTP / MOTP / STEAM |
| 算法 | SHA1 / SHA256 / SHA512 |
| 导入方式 | 扫描二维码、手动添加 |
| 检索 | 主界面按 issuer / label / tag / type / algorithm 模糊搜索 |
| 本地安全 | 主密码（`PBKDF2WithHmacSHA256` + Android Keystore + `EncryptedSharedPreferences`）加密存储 |
| 启动锁 | 指纹 / 面容 / 密码锁定 |
| 备份同步 | Git 加密备份与多设备同步（JGit + HTTPS Token） |
| 在线更新 | 应用内检查更新、下载并一键安装 |
| 崩溃反馈 | 自动记录崩溃堆栈，可复制并提交到项目 Issues |

## 截图

> 暂无截图，后续补充。

## 环境要求

- **JDK 17**
- **Android SDK**：`platforms;android-34`、`build-tools;34.0.0`
- **Gradle 8.9**（可使用仓库自带 wrapper 自动下载，或使用本机已安装的 Gradle）

## 从源码构建

1. 安装 JDK 17 与 Android SDK（需包含 `platforms;android-34`、`build-tools;34.0.0`）。
2. 配置 SDK 路径（二选一）：
   - 创建 `local.properties`，写入 `sdk.dir=你的SDK路径`（例如 `D:/Android/Sdk`）；或
   - 设置环境变量 `ANDROID_HOME` 指向 SDK 目录。
3. 构建：
   ```bash
   ./gradlew assembleRelease      # Linux / macOS
   gradlew.bat assembleRelease    # Windows
   ```
4. 产物：`app/build/outputs/apk/release/LXJ-MFA-vX.Y.Z-release.apk`

> 提示：覆盖安装不会刷新 launcher 图标（Android 缓存所致），如需显示新图标，卸载重装或清除桌面缓存即可。

## 使用简介

应用内「设置 → 使用说明」提供完整使用指引。

## 贡献

欢迎通过 Issue / Pull Request 参与贡献：

1. Fork 本仓库并提交改动；
2. 如遇崩溃，请在「设置 → 查看崩溃日志」中复制日志，并附到 [Issues](https://gitee.com/luan_xiaojian/lxj-mfa/issues) 中反馈。

## 许可证

[MIT](LICENSE) © 2026 LXJ
