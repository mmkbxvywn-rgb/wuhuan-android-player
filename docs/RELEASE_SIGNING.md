# 无患影音正式签名配置

本项目的 Android 正式签名密钥必须长期固定并由项目所有者备份。**不要将 Keystore 或密码提交到 Git 仓库，也不要在聊天中发送密码。**

## GitHub Actions Secrets

在仓库 Settings → Secrets and variables → Actions → New repository secret 中配置：

| Secret 名称 | 内容 |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Keystore 文件整体的 Base64 文本 |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore 密码 |
| `ANDROID_KEY_ALIAS` | 创建密钥时设置的 alias |
| `ANDROID_KEY_PASSWORD` | alias 对应的密钥密码 |

四项都配置后，后续 Actions 构建会自动使用正式签名。未全部配置时，工作流会明确标记为 debug 测试签名。

## 生成密钥

在可信任的本地电脑安装 JDK 后，使用 keytool 生成 JKS。以下命令会交互式询问密码和证书信息：

```bash
keytool -genkeypair -v -keystore wuhuan-release.jks -alias wuhuan -keyalg RSA -keysize 4096 -validity 10000
```

请妥善保存 `wuhuan-release.jks`，至少保留两份离线备份。不要重新生成替代它。

将文件转为 Base64 后，把完整输出粘贴到 `ANDROID_KEYSTORE_BASE64`：

- macOS：`base64 < wuhuan-release.jks | tr -d '\\n'`
- Linux：`base64 -w 0 wuhuan-release.jks`
- Windows PowerShell：`[Convert]::ToBase64String([IO.File]::ReadAllBytes("wuhuan-release.jks"))`

密码和 alias 分别填入对应 Secrets。建议 Keystore 密码与 key password 使用相同的强随机密码，便于管理；密码应保存在密码管理器和离线备份中。

## 升级验证

正式签名配置完成后：

1. 在 Actions 中确认产物名称包含 `release-signed`。
2. 下载 APK，使用 Android SDK 的 `apksigner verify --print-certs` 检查证书指纹。
3. 安装该版本；提高 versionCode 后再次构建。
4. 确认第二版证书指纹一致，并能直接覆盖安装第一版，应用数据仍在。

**注意：** 更换签名证书通常会阻止普通覆盖升级。丢失 Keystore 或密码可能导致无法继续向已安装用户发布可覆盖升级的版本。
