# 无患影音：源码基线审计（2026-11-23）

## 审计范围

- 基线分支：`dev-5.6.1`
- 工作分支：`wuhuan/development`
- 父仓库：`cyj265/lanxing-vod`
- 审计方式：读取 GitHub 当前分支中的 README、Gradle 配置、主 Manifest、App.java、Updater.java、依赖版本目录和 GitHub Actions 工作流。
- 本文只记录已检查内容；未完成全仓库依赖、网络请求和权限调用审计。

## 基础事实

- 工程是 Android 手机端 Java 项目，基于 FongMi/TV 二次开发。
- 主播放内核为 Media3 / ExoPlayer；MPV 已移除。
- 默认构建 flavor 为 mobile + arm64-v8a；leanback flavor 当前停用。
- Gradle Wrapper 指向 Gradle 9.5.1；源码要求 JDK 21、Android SDK、Python 3.10。
- 项目许可证为 GPL-3.0。衍生工程继续分发时必须履行 GPL-3.0 对应义务。
- 仓库含多个模块及本地 AAR / Maven 依赖，不能仅凭根目录大小判断最终 APK 体积。

## 已确认需要处理的项目

### 1. Bugly 崩溃遥测

证据：
- `app/build.gradle` 声明 `com.tencent.bugly:crashreport:4.1.9.3` 与 `nativecrashreport:3.9.2`。
- `App.java` 导入并调用 Bugly CrashReport；从 BuildConfig BUGLY_APP_ID 和官方签名校验决定是否初始化。
- GitHub Actions 工作流仍含 Bugly mapping 符号表上传步骤。

处理方向：
- 移除 Bugly 两项依赖、初始化代码、相关配置字段和 CI 符号表上传步骤。
- 保留本地崩溃日志能力前，检查日志内容、保存位置与清理策略。
- 完成全仓库引用检索后再提交，避免残留 import、配置或 CI Secret 依赖。

### 2. 第三方自动更新

证据：
- `Updater.java` 请求原作者仓库 GitHub Releases 与 Gitee Releases。
- 代码包含 GitHub 加速代理地址及 APK 下载候选逻辑。
- README 将该功能列为项目特性。

处理方向：
- 默认移除原作者更新检查、下载和安装入口。
- 如果未来需要无患影音自己的更新机制，另行设计为用户主动触发、来源明确、校验签名的独立功能；不沿用原作者更新源。

### 3. 权限与网络安全配置

主 Manifest 已声明：
- INTERNET、网络状态与 Wi-Fi 状态权限；
- READ/WRITE_EXTERNAL_STORAGE（带 SDK 限制）；
- MANAGE_EXTERNAL_STORAGE；
- REQUEST_INSTALL_PACKAGES；
- CHANGE_WIFI_MULTICAST_STATE、NEARBY_WIFI_DEVICES、ACCESS_LOCAL_NETWORK；
- FOREGROUND_SERVICE / FOREGROUND_SERVICE_MEDIA_PLAYBACK；
- POST_NOTIFICATIONS。
- application 设置 `usesCleartextTraffic=true`。

处理方向：
- 先逐项追踪调用点与功能依赖，再按最小权限原则收紧。
- 本地媒体优先评估 Storage Access Framework / MediaStore 能否替代广泛文件访问。
- 移除自动 APK 更新后，重新评估 REQUEST_INSTALL_PACKAGES。
- 明文 HTTP 是否需要由用户配置的影视源兼容性决定；不能在未确认兼容影响前直接关闭。
- DLNA/SSDP 相关权限需结合实际投屏功能测试后决定。

### 4. CI 发布副作用

证据：
- `.github/workflows/build.yml` 对 `main` 与 `dev-5.6.1` 的 push 会触发。
- 工作流除构建 APK 外，还会创建 GitHub Release、清理旧 Release，并尝试上传 Bugly mapping。
- 当前工作分支 `wuhuan/development` 不在该 push 触发分支列表内。

处理方向：
- 在无患影音分支建立独立 CI：只构建并上传 Actions Artifact，不创建或删除 Release，不上传 Bugly 数据。
- 在审查完 workflow 与 secrets 引用前，不把构建任务接到原工作流上。
- 不在用户未明确要求时改动原作者的 Release、Tag 或其他分支。

## 当前不能声称的事项

- 尚未完成全仓库第三方 SDK / 埋点 / 网络请求审计。
- 尚未完成所有 Manifest 合并结果和运行时权限调用审计。
- 尚未在本环境构建 APK，也未在 Android 设备上验证播放。
- 尚未实现广告过滤；README 对广告过滤没有可作为实现证据的说明。
- 尚未确认所有影视仓 Spider 类型在当前分支的兼容边界。

## 下一阶段

1. 检索全仓库 Bugly、更新器、广告、统计与网络请求引用。
2. 核实应用启动、播放、源配置、Spider 执行链路，避免清理时破坏核心播放能力。
3. 设计独立的无患影音构建工作流（仅 Artifact）。
4. 形成最小权限清单与广告过滤技术方案，再逐项实施。


## 2026-11-23：第一批清理后的状态

- 已从应用构建依赖与启动代码中移除 Bugly CrashReport / NativeCrashReport 及其初始化链。
- 已移除手机首页自动更新检查、关于页更新入口，以及原作者更新器实现和旧发布工作流。
- 本地 CrashLog 仍保留；其日志字段、目录、容量限制和清理机制仍需单独审计。
- 以上仅清理已定位的 Bugly 与原作者更新链，不代表全仓网络请求、第三方 SDK、设备标识或权限审计已经完成。
