<p align="center">
  <img src="docs/images/ic_launcher.png" alt="曼播 Logo" width="120" />
</p>

<h1 align="center">Manbo 曼播 🐱</h1>

<p align="center">
  <strong>一个简单可爱的本地视频播放器喵～</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B-brightgreen?logo=android" alt="Platform" />
  <img src="https://img.shields.io/badge/Kotlin-1.x-blue?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=android" alt="Compose" />
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License" />
</p>

## 简介

曼播是一个 Android 本地视频播放器，用最简洁的设计给你最舒适的观影体验喵～ ✨

<p align="center">
  <img src="docs/images/splash_icon.png" alt="曼播" width="200" />
</p>

## 功能特性

- 🎬 本地视频播放，支持多种格式喵
- 📺 画中画模式，边看边玩不是梦喵
- 🖼️ 横屏全屏播放，沉浸式体验喵
- 💾 Room 数据库管理播放记录喵
- 🎨 Material 3 设计，界面清爽可爱喵
- 📱 支持 Android 7.0+（API 24+）喵

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **播放器**: GSYVideoPlayer + ExoPlayer2
- **图片加载**: Coil
- **数据库**: Room
- **最低 SDK**: 24 (Android 7.0)
- **目标 SDK**: 36

## 本地构建

以下命令默认都在项目根目录执行，也就是当前这个 `README.md` 所在目录。

```bash
./gradlew assembleRelease
```

## 自动发版（GitHub Actions）

项目已配置 GitHub Actions，推送 tag 即可自动打包并发布 Release 喵～ 🎉

### 1. 配置 GitHub Secrets

进入仓库 **Settings → Secrets and variables → Actions → New repository secret**，添加以下 4 个 Secret：

| Secret 名 | 含义 | 当前项目默认值 / 生成方式 |
|---|---|---|
| `KEYSTORE_BASE64` | 签名文件的 Base64 内容 | 从 `app/manbo-release.jks` 生成 |
| `STORE_PASSWORD` | Keystore 密码 | `manbo2026` |
| `KEY_PASSWORD` | Key 密码 | `manbo2026` |
| `KEY_ALIAS` | Key 别名 | `manbo` |

当前项目本地签名配置文件是 [keystore.properties](/Users/fim98/Documents/videoplayer/keystore.properties:1)，内容如下：

```properties
storePassword=manbo2026
keyPassword=manbo2026
keyAlias=manbo
storeFile=../app/manbo-release.jks
```

### 2. 签名文件路径

Release 签名文件路径固定为：

```text
app/manbo-release.jks
```

先确认这个文件在项目根目录下存在：

```bash
ls -l app/manbo-release.jks
```

### 3. 如果本地没有 keystore，如何生成

如果 `app/manbo-release.jks` 不存在，可以在项目根目录执行下面命令重新生成：

```bash
keytool -genkeypair \
  -v \
  -keystore app/manbo-release.jks \
  -alias manbo \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass manbo2026 \
  -keypass manbo2026 \
  -dname "CN=Manbo, OU=Manbo, O=Manbo, L=Unknown, ST=Unknown, C=CN"
```

如果你生成了新的 keystore，记得同时更新 GitHub Secrets 里的 4 个值，不能只换 `KEYSTORE_BASE64`。

### 4. 生成 KEYSTORE_BASE64

在项目根目录执行下面命令，把单行 Base64 复制到剪贴板：

```bash
base64 -i app/manbo-release.jks | tr -d '\n' | pbcopy
```

如果你想直接在终端里看内容，也可以执行：

```bash
base64 -i app/manbo-release.jks | tr -d '\n'
```

可以再用下面两个命令校验：

```bash
pbpaste | head -c 80
shasum -a 256 app/manbo-release.jks
```

当前仓库这份 keystore 的 SHA-256 是：

```text
5c0424b9b817dddd4435942d38118330834d48a1532164e7aeedb0478dec5eb9
```

### 5. 本地验证签名配置

推送 tag 之前，建议先验证本地 keystore 能否正常读取：

```bash
keytool -list -v \
  -keystore app/manbo-release.jks \
  -storepass manbo2026 \
  -alias manbo
```

如果这里报错，GitHub Actions 的 release 构建也会失败。

### 6. 触发发版

```bash
# 打 tag（版本号按实际修改）
git tag v1.2

# 推送 tag 到远程
git push origin v1.2
```

推送后进入 **Actions** 页面可以看到 `Release Build` 工作流正在运行。构建完成后，APK 会自动挂载到对应版本的 **Releases** 页面喵～ 🐾

### 7. 下载 APK

进入仓库 **Releases** 页面，找到对应版本的 `Manbo-release-vX.X.apk` 下载安装就好啦喵～

## License

MIT

---

<p align="center">
  慢慢看，慢慢播，生活就该慢慢来喵～ 🐾
</p>
