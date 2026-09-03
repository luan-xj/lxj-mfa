# LXJ-MFA

> 一款干净、轻量、秒开的 Android 双因素认证（2FA / MFA）工具。
> 无广告、无追踪、数据只留在你的设备和你的 Git 仓库里，绿色安全。

LXJ-MFA 用原生 Kotlin 写成，支持 TOTP / HOTP / MOTP / STEAM 等常见动态口令算法。扫码即添加、启动即锁、本地加密存储，加密备份同步完全走你自己的仓库，不经过任何第三方服务器。
## 欢迎使用浏览器插件版本：https://gitee.com/LXJ1203/lxj-mfa-extension.git
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
  <img src="https://gitee.com/LXJ1203/lxj-mfa/raw/master/screenshots/locked.jpg" alt="锁定" width="160" height="auto" />
  <img src="https://gitee.com/LXJ1203/lxj-mfa/raw/master/screenshots/home.jpg" alt="主界面" width="160" height="auto" />
  <img src="https://gitee.com/LXJ1203/lxj-mfa/raw/master/screenshots/add.jpg" alt="添加账号" width="160" height="auto" />
  <img src="https://gitee.com/LXJ1203/lxj-mfa/raw/master/screenshots/data.jpg" alt="数据与备份" width="160" height="auto" />
  <img src="https://gitee.com/LXJ1203/lxj-mfa/raw/master/screenshots/menu.jpg" alt="菜单" width="160" height="auto" />
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
| 备份同步 | Git 加密备份与多设备同步（JGit + HTTPS Token，支持 Gitee / GitHub / GitLab / AtomGit / 自建 Git） |
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

## 支持的 Git 平台与常见踩坑

### 同步原理

LXJ-MFA 的 Git 同步基于 [JGit](https://eclipse.dev/jgit/)，使用 **HTTPS + 个人访问令牌（PAT）** 进行 `clone / pull / push`，不依赖任何特定平台的私有 API。因此只要是「支持 HTTPS + Token 鉴权」的 Git 托管服务都可以用，包括：

| 平台 | 可用性 | 注意事项 |
| --- | --- | --- |
| Gitee | ✅ 已实测 | Token 需有仓库读写权限 |
| GitHub | ✅ 可用 | Token 需 `repo` 权限；新仓库默认分支多为 `main` |
| GitLab | ✅ 可用 | Token 需 `write_repository` 权限；新仓库默认分支多为 `main` |
| AtomGit | ✅ 可用 | 标准 Git 托管 + Token 鉴权 |
| 自建 Git（Gogs / Gitea / Forgejo 等） | ✅ 可用 | 填对应 HTTPS 地址与有权限的 Token 即可 |

> 注：应用自身的「在线更新检查」固定指向本项目仓库（`LXJ1203/lxj-mfa`），与上面「用户数据同步」是两套独立机制。

### Git 用户名

HTTPS Token 认证时，绝大多数平台会忽略用户名，因此「Git 用户名」可留空或填任意值（默认 `lxj-mfa`）。少数平台可能要求用户名与令牌所属账号一致，若遇到 `401` 鉴权失败，请填入你的登录用户名。

### 常见踩坑

1. **仓库要先自己建好**：同步只会 `clone / push`，不会自动创建仓库。请先在目标平台新建仓库（建议保留默认分支的初始提交），并确认分支名。
2. **分支名对不上**：设置里默认分支是 `main`，但 Gitee 旧仓库默认 `master`、个别平台可能不同。分支不存在会导致 `pull` 失败、或 `push` 推不上去。请填与目标仓库实际默认分支一致的值。
3. **只支持 HTTPS，不支持 SSH**：地址必须填 `https://...`，不要填 `git@...` 或 `ssh://...`。
4. **空仓库首次同步**：若远端仓库完全是空的（连一次提交都没有），首次 `clone` 可能失败。建议远端先有一个初始提交（哪怕只有一个 README），克隆成功后本机会把 `lxj-mfa-backup.json` 提交并推送。
5. **Token 权限不足**：返回 `401 / 403` 通常是 Token 没有写权限，或已过期 / 被撤销。请确认 Token 具备目标仓库的读写权限。
6. **明文备份会永久留痕**：若关闭「同步时加密备份」，账号密钥将以明文写入 Git 历史，且无法撤销。强烈建议保持开启并设置「同步密码」（独立于本地主密码）。
7. **自签名证书**：极少数自建 Git 使用自签名证书时，JGit 默认会拒绝。需在该 Git 服务侧配置受信任证书，或在客户端显式信任该证书（高级操作，需自行处理）。

## 贡献

欢迎通过 Issue / Pull Request 参与贡献：

1. Fork 本仓库并提交改动；
2. 如遇崩溃，请在「设置 → 查看崩溃日志」中复制日志，并附到 [Issues](https://gitee.com/LXJ1203/lxj-mfa/issues) 中反馈。

## 欢迎使用浏览器插件版本：https://gitee.com/LXJ1203/lxj-mfa-extension.git

## 许可证

[MIT](LICENSE) © 2026 LXJ
