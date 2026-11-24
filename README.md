# 无患影音

无患影音是一款面向 Android 手机和平板的本地与网络影音播放器。项目以成熟的 Android 播放与影视源兼容代码为技术底座，逐步形成独立的产品界面、隐私策略和维护体系。

**应用不捆绑影视内容文件或 Spider 可执行代码。** 当前开发阶段会内置一个可替换、可删除的公开 CMS 测试源配置，用于验证资源加载链路；它引用第三方服务，不代表项目拥有内容授权或保证服务稳定。用户也可以自行导入其他兼容配置。

## 产品原则

- 无广告 SDK、广告位、开屏推广和强制会员。
- 默认不向第三方发送崩溃遥测、设备标识或使用行为数据。
- 不自动检查原作者更新，不自动下载或安装第三方 APK。
- 本地媒体与用户主动配置的网络媒体是核心功能。
- 开发版的默认测试源仅用于协议验证，可由用户删除或替换；正式发布前重新评估来源、内容类别和稳定性。
- 不承诺所有网站、Spider 或编码格式均可播放；兼容性以实际测试为准。

> 应用本身不展示广告，不代表第三方视频源或视频内容中不存在广告。播放器无法保证去除已经合成在视频画面中的广告。

## 当前技术基线

- Android 手机 / 平板优先；Android TV 后续单独适配。
- 播放内核：AndroidX Media3 / ExoPlayer。
- 影视源生态：TVBox JSON 配置及 CatVod Spider 兼容架构。
- 当前内部 Java namespace 仍沿用上游包路径以降低迁移风险；独立安装包 ID 为 `com.wuhuan.player`。
- 最低 Android 版本以 Gradle 配置为准。
- 项目许可证：GPL-3.0。分发衍生版本时，需履行许可证、版权声明及对应源码提供义务。

## 构建

构建环境：

- JDK 21
- Android SDK（compileSdk 版本见 `gradle/libs.versions.toml`）
- Python 3.10（Chaquopy 模块需要）

手机版 ARM64 构建命令：

```bash
./gradlew :app:assembleMobileArm64_v8aRelease
```

APK 输出目录：

```
app/build/outputs/apk/mobileArm64_v8a/release/
```

仓库 GitHub Actions 工作流只构建并上传临时 Artifact，不自动创建 Release、删除 Release 或发布更新。未配置正式签名密钥时，构建产物使用 debug 签名，仅用于测试。

## 影视源配置

当前版本沿用底座已有的 TVBox JSON / Spider 配置能力。开发版首次安装会加载一个应用内的测试配置文件，该文件引用公开 CMS API；用户可以删除或替换它。第三方接口的可用性、内容授权和持续稳定性不由项目保证。不同配置可能依赖不同网络协议、脚本运行环境和第三方服务，不能仅凭“导入成功”认定所有站点均可正常播放。

后续将补充无患影音自己的配置管理说明、兼容性清单和可重复测试样例。

## 项目文档

- [项目计划](docs/PROJECT_PLAN.md)
- [工作日志](docs/WORK_LOG.md)
- [源码基线审计](docs/BASELINE_AUDIT.md)
- [UI 结构审计](docs/UI_BASELINE_AUDIT.md)
- [配置体系审计](docs/CONFIG_AUDIT.md)

## 致谢与来源

本项目基于 [FongMi/TV](https://github.com/FongMi/TV) 及 [cyj265/lanxing-vod](https://github.com/cyj265/lanxing-vod) 的开源代码开展二次开发。保留并尊重上游许可证与版权声明。无患影音的独立改造以本仓库工作分支为准。

## 免责声明

本应用仅提供媒体播放与用户配置管理能力，不提供影视内容、播放地址或第三方内容服务。用户应确保自行配置和播放的内容来源合法合规。
