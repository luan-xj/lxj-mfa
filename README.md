# LXJ-MFA

> 一款干净、轻量、秒开的 Android 双因素认证（2FA / MFA）工具。
> 无广告、无追踪、数据只留在你的设备和你的 Git 仓库里，绿色安全。

LXJ-MFA 用原生 Kotlin 写成，支持 TOTP / HOTP / MOTP / STEAM 等常见动态口令算法。扫码即添加、启动即锁、本地加密存储，备份同步完全走你自己的仓库，不经过任何第三方服务器。

## 为什么选择 LXJ-MFA？

| 特点 | 说明 |
| --- | --- |
| **无广告** | 纯本地工具，没有开屏、横幅、推送和会员诱导。 |
| **快速打开** | 原生 Kotlin + Material 3，秒级启动，列表滑动不卡顿。 |
| **数据只在你手里** | 主密码经 `PBKDF2` 与 Android Keystore 派生密钥；凭据使用 `EncryptedSharedPreferences` 加密，本地存储，不上传。 |
| **安全绿色** | 支持指纹 / 面容 / 密码启动锁；MIT 开源，无任何埋点与追踪。 |

## 截图

这些截图基于真实布局与配色渲染，展示应用的核心界面：

<div align="center">
  <img src="https://gitee.com/LXJ1203/lxj-mfa/raw/master/screenshots/home.png" alt="主界面" width="280" />
  <img src="https://gitee.com/LXJ1203/lxj-mfa/raw/master/screenshots/add.png" alt="添加账号" width="280" />
  <img src="https://gitee.com/LXJ1203/lxj-mfa/raw/master/screenshots/data.png" alt="数据与备份" width="280" />
</div>

## 功能特性

| 类别 | 说明 |
| --- | --- |
| 账号类型 | TOTP / HOTP / MOTP / STEAM |
| 算法 | SHA1 / SHA256 / SHA512 |
| 导入方式 | 扫描二维码、手动添加 |
| 检索 | 按 issuer / label / tag / type / algorithm 模糊搜索 |
| 本地安全 | 主密码 + `PBKDF2WithHmacSHA256` + Android Keystore + `EncryptedSharedPreferences` |
| 启动锁 | 指纹 / 面容 / 密码锁定 |
| 备份同步 | Git 加密备份与多设备同步（JGit + HTTPS Token） |
| 在线更新 | 应用内检查更新、下载并一键安装 |
| 崩溃反馈 | 自动记录崩溃堆栈，可复制并提交到项目 Issues |

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
