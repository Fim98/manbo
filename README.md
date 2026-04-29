# Manbo 曼播 🐱

> 一个简单可爱的本地视频播放器喵～

## 简介

Manbo（曼播）是一个 Android 本地视频播放器，用最简洁的设计给你最舒适的观影体验喵～ ✨

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

```bash
./gradlew assembleRelease
```

## 自动发版（GitHub Actions）

项目已配置 GitHub Actions，推送 tag 即可自动打包并发布 Release 喵～ 🎉

### 1. 配置 GitHub Secrets

进入仓库 **Settings → Secrets and variables → Actions → New repository secret**，添加以下 4 个 Secret：

| Secret 名 | 说明 |
|---|---|
| `KEYSTORE_BASE64` | 签名文件 `manbo-release.jks` 的 Base64 编码 |
| `STORE_PASSWORD` | Keystore 密码 |
| `KEY_PASSWORD` | Key 密码 |
| `KEY_ALIAS` | Key 别名 |

### 2. 生成 KEYSTORE_BASE64

在项目根目录执行：

```bash
base64 -i app/manbo-release.jks | pbcopy
```

> 执行后不会有输出，Base64 内容已经复制到剪贴板了喵～ 直接粘贴到 GitHub Secrets 就好 ✨

验证是否复制成功：

```bash
pbpaste | head -c 50
```

### 3. 触发发版

```bash
# 打 tag（版本号按实际修改）
git tag v1.0

# 推送 tag 到远程
git push origin v1.0
```

推送后进入 **Actions** 页面可以看到 `Release Build` 工作流正在运行。构建完成后，APK 会自动挂载到对应版本的 **Releases** 页面喵～ 🐾

### 4. 下载 APK

进入仓库 **Releases** 页面，找到对应版本的 `Manbo-release-vX.X.apk` 下载安装就好啦喵～

## License

MIT

---

慢慢看，慢慢播，生活就该慢慢来喵～ 🐾
